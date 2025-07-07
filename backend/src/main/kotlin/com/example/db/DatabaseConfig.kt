package com.example.db

import com.example.tables.Budgets
import com.example.tables.Users

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun configureDatabase() {
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
}