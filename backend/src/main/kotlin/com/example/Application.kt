package com.example

import com.example.routes.registerBudgetRoutes
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.response.respondText
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    Database.connect(
        url = "jdbc:postgresql://localhost:5432/budget_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "password"
    )

    routing {
        get("/") {
            call.respondText("Budget Planner API is running!")
        }
    }

    registerBudgetRoutes() 
}

