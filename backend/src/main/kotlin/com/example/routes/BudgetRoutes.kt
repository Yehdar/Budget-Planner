package com.example.routes

import com.example.model.*

import com.example.service.BudgetService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureBudgetRoutes(budgetService: BudgetService) {
    routing {
        post("/budget/addCategory") {
            try {
                val request = call.receive<AddBudgetCategoryRequest>()
                val result = budgetService.addOrUpdateCategory(request)
                when (result) {
                    "updated" -> call.respond(HttpStatusCode.OK, "Budget category '${request.categoryName}' updated successfully.")
                    "created" -> call.respond(HttpStatusCode.Created, "Budget category '${request.categoryName}' added successfully.")
                    else -> call.respond(HttpStatusCode.InternalServerError, "Unexpected error during category operation.")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Failed to add/update budget category: ${e.localizedMessage}")
            }
        }

        post("/budget/recordSpend") {
            try {
                val request = call.receive<RecordSpendRequest>()
                val transactionResult = budgetService.recordSpend(request)
                when {
                    transactionResult == "notFound" -> call.respond(HttpStatusCode.NotFound, "Budget category '${request.categoryName}' not found for user.")
                    transactionResult.startsWith("success:") -> {
                        val newSpent = transactionResult.split(":")[1]
                        call.respond(HttpStatusCode.OK, "Spend recorded for '${request.categoryName}'. New spent total: $newSpent")
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred.")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Failed to record spending: ${e.localizedMessage}")
            }
        }

        delete("/budget/deleteCategory/{categoryName}") {
            try {
                val categoryName = call.parameters["categoryName"] ?: throw IllegalArgumentException("Category name missing")
                val deletedRows = budgetService.deleteCategory(categoryName)
                if (deletedRows > 0) {
                    call.respond(HttpStatusCode.OK, "Budget category '$categoryName' deleted successfully.")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Budget category '$categoryName' not found for user.")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete budget category: ${e.localizedMessage}")
            }
        }

        get("/budget/getAllCategories") {
            try {
                val categories = budgetService.getAllCategories()
                call.respond(HttpStatusCode.OK, categories)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve budget categories: ${e.localizedMessage}")
            }
        }

        get("/budget/getCategoryDetails/{categoryName}") {
            try {
                val categoryName = call.parameters["categoryName"] ?: throw IllegalArgumentException("Category name missing")
                val categoryDetails = budgetService.getCategoryDetails(categoryName)
                if (categoryDetails != null) {
                    call.respond(HttpStatusCode.OK, categoryDetails)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Category '$categoryName' not found for user.")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve category details: ${e.localizedMessage}")
            }
        }
    }
}