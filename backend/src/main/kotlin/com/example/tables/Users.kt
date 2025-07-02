package com.example.tables

import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 64) // in real life: hash!
    val balance = decimal("balance", 10, 2)

    override val primaryKey = PrimaryKey(id)
}
