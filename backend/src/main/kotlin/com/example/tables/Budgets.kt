package com.example.tables

import org.jetbrains.exposed.sql.Table

object Budgets : Table() {
    val id = integer("id").references(Users.id) // not primaryKey here
    val values = text("values")
}
