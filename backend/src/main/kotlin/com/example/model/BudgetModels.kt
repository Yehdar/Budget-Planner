package com.example.model

import kotlinx.serialization.Serializable

// Data class for adding a new budget category
@Serializable
data class AddBudgetCategoryRequest(
    val categoryName: String,
    val originalValue: Double
)

// Data class for recording a spending transaction
@Serializable
data class RecordSpendRequest(
    val categoryName: String,
    val amountSpent: Double,
    val description: String
)

// Data class for a single transaction entry in the history
@Serializable
data class TransactionEntry(
    val amount: Double,
    val description: String
)

// Data class to represent a budget category item for response
@Serializable
data class BudgetCategoryItem(
    val category: String,
    val originalValue: Double,
    val spentAmountSoFar: Double,
    val transactionHistory: Map<String, List<TransactionEntry>>
)