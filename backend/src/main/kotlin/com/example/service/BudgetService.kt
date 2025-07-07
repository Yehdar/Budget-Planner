package com.example.service

import com.example.tables.Budgets
import com.example.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate


class BudgetService {

    private val defaultUserId = 1

    fun addOrUpdateCategory(request: AddBudgetCategoryRequest): String = transaction {
        val existingCategory = Budgets.select {
            (Budgets.userId eq defaultUserId) and (Budgets.category eq request.categoryName)
        }.singleOrNull()

        if (existingCategory != null) {
            Budgets.update({ (Budgets.userId eq defaultUserId) and (Budgets.category eq request.categoryName) }) {
                it[Budgets.originalValue] = request.originalValue
            }
            "updated"
        } else {
            Budgets.insert {
                it[Budgets.userId] = defaultUserId
                it[Budgets.category] = request.categoryName
                it[Budgets.originalValue] = request.originalValue
                it[Budgets.spentAmountSoFar] = 0.0
                it[Budgets.transactionHistory] = "{}"
            }
            "created"
        }
    }

    fun recordSpend(request: RecordSpendRequest): String = transaction {
        val existingCategory = Budgets.select {
            (Budgets.userId eq defaultUserId) and (Budgets.category eq request.categoryName)
        }.singleOrNull()

        if (existingCategory == null) {
            return@transaction "notFound"
        }

        val currentSpent = existingCategory[Budgets.spentAmountSoFar]
        val newSpent = currentSpent + request.amountSpent

        val currentHistoryJsonString = existingCategory[Budgets.transactionHistory]
        val currentHistory: MutableMap<String, MutableList<TransactionEntry>> =
            if (currentHistoryJsonString.isNotEmpty() && currentHistoryJsonString != "{}") {
                Json.decodeFromString(currentHistoryJsonString)
            } else {
                mutableMapOf()
            }

        val currentDate = LocalDate.now().toString()

        val transactionsForDate = currentHistory.getOrPut(currentDate) { mutableListOf() }
        transactionsForDate.add(TransactionEntry(request.amountSpent, request.description))

        val updatedHistoryJsonString = Json.encodeToString(currentHistory)

        Budgets.update({ (Budgets.userId eq defaultUserId) and (Budgets.category eq request.categoryName) }) {
            it[Budgets.spentAmountSoFar] = newSpent
            it[Budgets.transactionHistory] = updatedHistoryJsonString
        }
        "success:$newSpent"
    }

    fun deleteCategory(categoryName: String): Int = transaction {
        Budgets.deleteWhere { (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName) }
    }

    fun getAllCategories(): List<BudgetCategoryItem> = transaction {
        Budgets.select { Budgets.userId eq defaultUserId }
            .map { row ->
                val historyJsonString = row[Budgets.transactionHistory]
                val historyMap: Map<String, List<TransactionEntry>> =
                    if (historyJsonString.isNotEmpty() && historyJsonString != "{}") {
                        Json.decodeFromString(historyJsonString)
                    } else {
                        emptyMap()
                    }

                BudgetCategoryItem(
                    category = row[Budgets.category],
                    originalValue = row[Budgets.originalValue],
                    spentAmountSoFar = row[Budgets.spentAmountSoFar],
                    transactionHistory = historyMap
                )
            }
    }

    fun getCategoryDetails(categoryName: String): BudgetCategoryItem? = transaction {
        Budgets.select {
            (Budgets.userId eq defaultUserId) and (Budgets.category eq categoryName)
        }.singleOrNull()?.let { row ->
            val historyJsonString = row[Budgets.transactionHistory]
            val historyMap: Map<String, List<TransactionEntry>> =
                if (historyJsonString.isNotEmpty() && historyJsonString != "{}") {
                    Json.decodeFromString(historyJsonString)
                } else {
                    emptyMap()
                }

            BudgetCategoryItem(
                category = row[Budgets.category],
                originalValue = row[Budgets.originalValue],
                spentAmountSoFar = row[Budgets.spentAmountSoFar],
                transactionHistory = historyMap
            )
        }
    }
}