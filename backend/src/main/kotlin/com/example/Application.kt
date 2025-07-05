package com.example

import com.example.tables.Users
import com.example.tables.Budgets

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.server.request.receive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq // ADD THIS IMPORT

// Data class for adding a new budget category
@Serializable
data class AddBudgetCategoryRequest(
    val categoryName: String,
    val originalValue: Double
)

// Data class for recording a spending transaction
@Serializable
data class RecordSpendRequest(
    val categoryName: String,
    val amountSpent: Double,
    val description: String
)

// Data class for a single transaction entry in the history
@Serializable
data class TransactionEntry(
    val amount: Double,
    val description: String
)

// Data class to represent a budget category item for response
@Serializable
data class BudgetCategoryItem(
    val category: String,
    val originalValue: Double,
    val spentAmountSoFar: Double,
    val transactionHistory: Map<String, List<TransactionEntry>>
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete) // Allow DELETE method for category deletion
        }

        install(ContentNegotiation) {
            json()
        }

        Database.connect(
            url = "jdbc:postgresql://localhost:5432/budget_db",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "password"
        )

        transaction {
            SchemaUtils.create(Users, Budgets)

            // Ensure a default user exists (ID 1) for budget association
            val defaultUser = Users.select { Users.id eq 1 }.singleOrNull()
            if (defaultUser == null) {
                Users.insert {
                    it[id] = 1
                    it[username] = "default_user"
                    it[password] = "default_password"
                }
            }
        }

        routing {
            // Endpoint to add a new budget category or update its original value
            post("/budget/addCategory") {
                try {
                    val request = call.receive<AddBudgetCategoryRequest>()
                    val categoryName = request.categoryName
                    val originalValue = request.originalValue
                    val defaultUserId = 1

                    val result = transaction {
                        val existingCategory = Budgets.select {
                            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
                        }.singleOrNull()

                        if (existingCategory != null) {
                            Budgets.update({ (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName) }) {
                                it[Budgets.originalValue] = originalValue
                            }
                            "updated"
                        } else {
                            Budgets.insert {
                                it[Budgets.userId] = defaultUserId
                                it[Budgets.category] = categoryName
                                it[Budgets.originalValue] = originalValue
                                it[Budgets.spentAmountSoFar] = 0.0
                                it[Budgets.transactionHistory] = "{}"
                            }
                            "created"
                        }
                    }

                    when (result) {
                        "updated" -> call.respond(HttpStatusCode.OK, "Budget category '$categoryName' updated successfully.")
                        "created" -> call.respond(HttpStatusCode.Created, "Budget category '$categoryName' added successfully.")
                        else -> call.respond(HttpStatusCode.InternalServerError, "Unexpected error during category operation.")
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to add/update budget category: ${e.localizedMessage}")
                }
            }

            // Endpoint to record spending for a category
            post("/budget/recordSpend") {
                try {
                    val request = call.receive<RecordSpendRequest>()
                    val categoryName = request.categoryName
                    val amountSpent = request.amountSpent
                    val description = request.description
                    val defaultUserId = 1

                    val transactionResult: String = transaction {
                        val existingCategory = Budgets.select {
                            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
                        }.singleOrNull()

                        if (existingCategory == null) {
                            return@transaction "notFound"
                        }

                        val currentSpent = existingCategory[Budgets.spentAmountSoFar]
                        val newSpent = currentSpent + amountSpent

                        val currentHistoryJsonString = existingCategory[Budgets.transactionHistory]
                        val currentHistory: MutableMap<String, MutableList<TransactionEntry>> =
                            if (currentHistoryJsonString.isNotEmpty() && currentHistoryJsonString != "{}") {
                                Json.decodeFromString(currentHistoryJsonString)
                            } else {
                                mutableMapOf()
                            }

                        val currentDate = LocalDate.now().toString()

                        val transactionsForDate = currentHistory.getOrPut(currentDate) { mutableListOf() }
                        transactionsForDate.add(TransactionEntry(amountSpent, description))

                        val updatedHistoryJsonString = Json.encodeToString(currentHistory)

                        Budgets.update({ (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName) }) {
                            it[Budgets.spentAmountSoFar] = newSpent
                            it[Budgets.transactionHistory] = updatedHistoryJsonString
                        }
                        "success:$newSpent"
                    }

                    when {
                        transactionResult == "notFound" -> call.respond(HttpStatusCode.NotFound, "Budget category '$categoryName' not found for user.")
                        transactionResult.startsWith("success:") -> {
                            val newSpent = transactionResult.split(":")[1]
                            call.respond(HttpStatusCode.OK, "Spend recorded for '$categoryName'. New spent total: $newSpent")
                        }
                        else -> call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred.")
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to record spending: ${e.localizedMessage}")
                }
            }

            // Endpoint to delete a budget category
            delete("/budget/deleteCategory/{categoryName}") {
                try {
                    val categoryName = call.parameters["categoryName"] ?: throw IllegalArgumentException("Category name missing")
                    val defaultUserId = 1

                    // The 'eq' operator needs to be explicitly imported from SqlExpressionBuilder
                    val deletedRows = transaction {
                        Budgets.deleteWhere { (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName) }
                    }

                    if (deletedRows > 0) {
                        call.respond(HttpStatusCode.OK, "Budget category '$categoryName' deleted successfully.")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Budget category '$categoryName' not found for user.")
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete budget category: ${e.localizedMessage}")
                }
            }

            // Endpoint to get all budget categories for the default user
            get("/budget/getAllCategories") {
                try {
                    val defaultUserId = 1

                    val categories = transaction {
                        Budgets.select { Budgets.userId eq defaultUserId }
                            .map { row ->
                                val historyJsonString = row[Budgets.transactionHistory]
                                val historyMap: Map<String, List<TransactionEntry>> =
                                    if (historyJsonString.isNotEmpty() && historyJsonString != "{}") {
                                        Json.decodeFromString(historyJsonString)
                                    } else {
                                        emptyMap()
                                    }

                                BudgetCategoryItem(
                                    category = row[Budgets.category],
                                    originalValue = row[Budgets.originalValue],
                                    spentAmountSoFar = row[Budgets.spentAmountSoFar],
                                    transactionHistory = historyMap
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, categories)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve budget categories: ${e.localizedMessage}")
                }
            }

            // Endpoint to get details for a specific budget category (including its transaction history)
            get("/budget/getCategoryDetails/{categoryName}") {
                try {
                    val categoryName = call.parameters["categoryName"] ?: throw IllegalArgumentException("Category name missing")
                    val defaultUserId = 1

                    val categoryDetails = transaction {
                        Budgets.select {
                            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
                        }.singleOrNull()?.let { row ->
                            val historyJsonString = row[Budgets.transactionHistory]
                            val historyMap: Map<String, List<TransactionEntry>> =
                                if (historyJsonString.isNotEmpty() && historyJsonString != "{}") {
                                    Json.decodeFromString(historyJsonString)
                                } else {
                                    emptyMap()
                                }

                            BudgetCategoryItem(
                                category = row[Budgets.category],
                                originalValue = row[Budgets.originalValue],
                                spentAmountSoFar = row[Budgets.spentAmountSoFar],
                                transactionHistory = historyMap
                            )
                        }
                    }

                    if (categoryDetails != null) {
                        call.respond(HttpStatusCode.OK, categoryDetails)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Category '$categoryName' not found for user.")
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve category details: ${e.localizedMessage}")
                }
            }
        }
    }.start(wait = true)
}
