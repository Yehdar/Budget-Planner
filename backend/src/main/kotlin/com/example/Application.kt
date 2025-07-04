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
import kotlinx.serialization.decodeFromString // For decoding JSON string back to Map
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.serializer // Import for String.serializer()

// Data class for saving a single budget item
@Serializable
data class BudgetSaveRequest(
    val categoryName: String,
    val initialValue: String // Keeping as String to match frontend input flexibility
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
                    it[password] = "default_password" // Still hash in real app!
                }
            }
        }

        routing {
            // Endpoint to save or update budget items for the default user
            post("/budget/save") {
                try {
                    val request = call.receive<BudgetSaveRequest>()
                    val categoryName = request.categoryName
                    val initialValue = request.initialValue

                    transaction {
                        val defaultUserId = 1 // Using the ID of the default user

                        // Try to find an existing budget for the default user
                        val existingBudget = Budgets.select { Budgets.id eq defaultUserId }.singleOrNull()

                        val currentBudgetValues: MutableMap<String, JsonElement> =
                            if (existingBudget != null) {
                                // Decode existing JSON string into a mutable map
                                Json.decodeFromString<MutableMap<String, JsonElement>>(existingBudget[Budgets.values])
                            } else {
                                mutableMapOf() // Start with an empty map if no budget exists
                            }

                        // Add or update the new category and its value
                        // Convert the initialValue String to a JsonPrimitive (String)
                        // Use String.serializer() to explicitly tell Json.encodeToJsonElement how to serialize a String
                        currentBudgetValues[categoryName] = Json.encodeToJsonElement(String.serializer(), initialValue)

                        // Encode the updated map back to a JSON string
                        val updatedValuesJsonString = Json.encodeToString(currentBudgetValues)

                        if (existingBudget != null) {
                            // Update the existing budget entry
                            Budgets.update({ Budgets.id eq defaultUserId }) {
                                it[values] = updatedValuesJsonString
                            }
                        } else {
                            // Insert a new budget entry for the default user
                            Budgets.insert {
                                it[id] = defaultUserId
                                it[values] = updatedValuesJsonString
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, "Budget item saved successfully.")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Failed to save budget item: ${e.localizedMessage}")
                }
            }

            // Endpoint to get all budget items for the default user
            get("/budget/get") {
                try {
                    val defaultUserId = 1 // Using the ID of the default user

                    val budget = transaction {
                        Budgets.select { Budgets.id eq defaultUserId }.singleOrNull()
                    }

                    if (budget != null) {
                        // Respond with the raw JSON string of budget values
                        call.respondText(budget[Budgets.values], contentType = ContentType.Application.Json)
                    } else {
                        // Respond with an empty JSON object if no budget found for the user
                        call.respondText("{}", contentType = ContentType.Application.Json)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve budget: ${e.localizedMessage}")
                }
            }
        }
    }.start(wait = true)
}
