package com.example.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object Budgets : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val amount = double("amount")
    override val primaryKey = PrimaryKey(id)
}

@Serializable
data class Budget(val id: Int = 0, val title: String, val amount: Double)

fun Application.registerBudgetRoutes() {
    transaction {
        SchemaUtils.create(Budgets)
    }

    routing {
        route("/api/budgets") {
            get {
                val budgets = transaction {
                    Budgets.selectAll().map {
                        Budget(
                            id = it[Budgets.id],
                            title = it[Budgets.title],
                            amount = it[Budgets.amount]
                        )
                    }
                }
                call.respond(budgets)
            }

            post {
                val budget = call.receive<Budget>()
                transaction {
                    Budgets.insert {
                        it[title] = budget.title
                        it[amount] = budget.amount
                    }
                }
                call.respondText("Budget item added successfully.")
            }

            delete("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    transaction {
                        Budgets.deleteWhere { Budgets.id eq id }
                    }
                    call.respondText("Budget item deleted.")
                } else {
                    call.respondText("Invalid ID.")
                }
            }
        }
    }
}
