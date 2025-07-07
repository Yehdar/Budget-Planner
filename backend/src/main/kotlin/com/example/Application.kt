package com.example

import com.example.db.configureDatabase
import com.example.routes.configureBudgetRoutes
import com.example.service.BudgetService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        // Configure CORS
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
        }

        // Configure Content Negotiation for JSON
        install(ContentNegotiation) {
            json()
        }

        // Initialize database connection and schema
        configureDatabase()

        // Initialize service layer
        val budgetService = BudgetService()

        // Configure routes, passing the service instance
        configureBudgetRoutes(budgetService)

    }.start(wait = true)
}