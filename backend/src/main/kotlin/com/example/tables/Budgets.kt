package com.example.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Budgets : Table() {
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val category = varchar("category", 255)
    val originalValue = double("original_value")
    val spentAmountSoFar = double("spent_amount_so_far").default(0.0)
    val transactionHistory = text("transaction_history").default("{}")

    override val primaryKey = PrimaryKey(userId, category)
}
