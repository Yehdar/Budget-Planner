package com.example.tables

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Defines the Users table in the database.
 * Inherits from IntIdTable to automatically get an auto-incrementing integer 'id' primary key.
 */
object Users : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 64)
}