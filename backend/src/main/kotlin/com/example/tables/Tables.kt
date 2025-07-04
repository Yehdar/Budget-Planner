//package com.example.tables
//
//import org.jetbrains.exposed.dao.id.IntIdTable
//import org.jetbrains.exposed.sql.Table
//import org.jetbrains.exposed.sql.ReferenceOption // Make sure this is imported if you use CASCADE
//
//object Users : IntIdTable("users") {
//    val username = varchar("username", 50).uniqueIndex()
//    val password = varchar("password", 64)
//}
//
//object Budgets : Table("budgets") {
//    // Corrected 'id' definition:
//    // It's an integer column named "id" that references the 'id' column of the Users table.
//    // onDelete = ReferenceOption.CASCADE is a good practice for foreign keys if you want to delete budgets when a user is deleted.
//    // uniqueIndex() ensures that each user can only have one budget entry (a one-to-one relationship).
//    val id = integer("id").references(Users.id, onDelete = ReferenceOption.CASCADE).uniqueIndex()
//    val values = text("values") // Storing JSON as text
//
//    // *** THIS IS THE MISSING PIECE ***
//    // Explicitly declare 'id' as the primary key for the Budgets table.
//    override val primaryKey = PrimaryKey(id)
//}