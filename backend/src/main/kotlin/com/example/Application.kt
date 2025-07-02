package com.example

import com.example.tables.Users
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal



fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
        }
        
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/budget_db",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "password"
        )
        
        transaction {
            SchemaUtils.create(Users)
        }


        routing {
            get("/hello") {
                call.respondText("Hello, Budget Planner!")
            }
            post("/user/create") {
                transaction {
                    Users.insert {
                        it[username] = "testuser"
                        it[password] = "testpass"
                        it[balance] = BigDecimal("100.00")
                    }
                }
                call.respondText("User created!")
            }
        }
    }.start(wait = true)
}
