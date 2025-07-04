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
import java.time.LocalDate // Import for LocalDate
import kotlinx.serialization.builtins.ListSerializer // Import for ListSerializer
import kotlinx.serialization.builtins.MapSerializer // Import for MapSerializer

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
    // Changed transactionHistory to map date string to a LIST of TransactionEntry
    val transactionHistory: Map<String, List<TransactionEntry>>
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
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
                    it[id] = 1 // Explicitly set ID to 1 for the default user
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
                    val defaultUserId = 1 // Assuming default user

                    val result = transaction {
                        val existingCategory = Budgets.select {
                            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
                        }.singleOrNull()

                        if (existingCategory != null) {
                            // Update existing category's original value
                            Budgets.update({ (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName) }) {
                                it[Budgets.originalValue] = originalValue
                            }
                            "updated" // Return a string to indicate update
                        } else {
                            // Add new category
                            Budgets.insert {
                                it[Budgets.userId] = defaultUserId
                                it[Budgets.category] = categoryName
                                it[Budgets.originalValue] = originalValue
                                it[Budgets.spentAmountSoFar] = 0.0 // New category starts with 0 spent
                                it[Budgets.transactionHistory] = "{}" // New category starts with empty JSON object for history
                            }
                            "created" // Return a string to indicate creation
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
                    val defaultUserId = 1 // Assuming default user

                    // Perform database operations in transaction and return a result
                    val transactionResult: String = transaction {
                        val existingCategory = Budgets.select {
                            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
                        }.singleOrNull()

                        if (existingCategory == null) {
                            return@transaction "notFound" // Indicate category not found
                        }

                        val currentSpent = existingCategory[Budgets.spentAmountSoFar]
                        val newSpent = currentSpent + amountSpent

                        // Decode existing transaction history into a map of date to LIST of transactions
                        val currentHistoryJsonString = existingCategory[Budgets.transactionHistory]
                        val currentHistory: MutableMap<String, MutableList<TransactionEntry>> =
                            if (currentHistoryJsonString.isNotEmpty() && currentHistoryJsonString != "{}") {
                                Json.decodeFromString(currentHistoryJsonString)
                            } else {
                                mutableMapOf()
                            }

                        // Get current date for transaction history key
                        val currentDate = LocalDate.now().toString() // Format: YYYY-MM-DD

                        // Get the list of transactions for the current date, or create a new list if none exists
                        val transactionsForDate = currentHistory.getOrPut(currentDate) { mutableListOf() }
                        // Add the new transaction to the list
                        transactionsForDate.add(TransactionEntry(amountSpent, description))

                        // Encode updated history back to JSON string
                        val updatedHistoryJsonString = Json.encodeToString(currentHistory)

                        // Update the budget category record
                        Budgets.update({ (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName) }) {
                            it[Budgets.spentAmountSoFar] = newSpent
                            it[Budgets.transactionHistory] = updatedHistoryJsonString
                        }
                        "success:$newSpent" // Indicate success and new spent amount
                    }

                    // Handle the result of the transaction outside the transaction block
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

            // Endpoint to get all budget categories for the default user
            get("/budget/getAllCategories") {
                try {
                    val defaultUserId = 1 // Assuming default user

                    val categories = transaction {
                        Budgets.select { Budgets.userId eq defaultUserId }
                            .map { row ->
                                val historyJsonString = row[Budgets.transactionHistory]
                                // Decode transaction history into a map of date to LIST of transactions
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
                    val defaultUserId = 1 // Assuming default user

                    val categoryDetails = transaction {
                        Budgets.select {
                            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
                        }.singleOrNull()?.let { row ->
                            val historyJsonString = row[Budgets.transactionHistory]
                            // Decode transaction history into a map of date to LIST of transactions
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
