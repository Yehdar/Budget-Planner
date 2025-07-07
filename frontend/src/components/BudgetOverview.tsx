import React from "react";
import type { BudgetCategoryItem } from "../models/types";

interface BudgetOverviewProps {
  budgetCategories: BudgetCategoryItem[];
  onDeleteCategory: (categoryName: string) => Promise<void>;
  isLoading: boolean;
}

const BudgetOverview: React.FC<BudgetOverviewProps> = ({
  budgetCategories,
  onDeleteCategory,
  isLoading,
}) => {
  return (
    <div className="bg-white p-6 rounded-lg shadow-md">
      <h2 className="text-2xl font-bold text-gray-800 mb-6">
        Your Budget Overview
      </h2>
      {isLoading && budgetCategories.length === 0 ? (
        <p className="text-gray-600 text-center py-8">Loading budget data...</p>
      ) : budgetCategories.length === 0 ? (
        <p className="text-gray-600 text-center py-8">
          No budget categories added yet. Use the form above to add one!
        </p>
      ) : (
        <div className="space-y-6">
          {budgetCategories.map((item) => (
            <div
              key={item.category}
              className="border border-gray-200 rounded-lg p-4 bg-gray-50"
            >
              <div className="flex justify-between items-center mb-3">
                <h3 className="text-xl font-semibold text-gray-800">
                  {item.category}
                </h3>
                <button
                  onClick={() => onDeleteCategory(item.category)}
                  className="px-3 py-1 bg-red-500 text-white text-sm rounded-md hover:bg-red-600 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50 transition duration-200"
                  disabled={isLoading}
                >
                  Delete
                </button>
              </div>
              <div className="text-gray-700 mb-2">
                <p>
                  Original Budget:{" "}
                  <span className="font-medium text-blue-600">
                    ${item.originalValue.toFixed(2)}
                  </span>
                </p>
                <p>
                  Spent So Far:{" "}
                  <span className="font-medium text-red-600">
                    ${item.spentAmountSoFar.toFixed(2)}
                  </span>
                </p>
                <p>
                  Remaining:{" "}
                  <span
                    className={`font-medium ${
                      item.originalValue - item.spentAmountSoFar >= 0
                        ? "text-green-600"
                        : "text-orange-600"
                    }`}
                  >
                    ${(item.originalValue - item.spentAmountSoFar).toFixed(2)}
                  </span>
                </p>
              </div>

              {Object.keys(item.transactionHistory).length > 0 && (
                <div className="mt-4 pt-4 border-t border-gray-200">
                  <h4 className="text-lg font-semibold text-gray-700 mb-2">
                    Transaction History:
                  </h4>
                  {Object.entries(item.transactionHistory).map(
                    ([date, transactions]) => (
                      <div key={date} className="mb-3">
                        <p className="font-medium text-gray-600 text-sm">
                          {date}
                        </p>
                        <ul className="list-disc list-inside ml-4 text-gray-600 text-sm">
                          {transactions.map((transaction, index) => (
                            <li key={index}>
                              ${transaction.amount.toFixed(2)} -{" "}
                              {transaction.description}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default BudgetOverview;
