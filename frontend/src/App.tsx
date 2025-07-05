import { useState, useEffect } from "react";

// Define the structure of a single transaction entry
interface TransactionEntry {
  amount: number;
  description: string;
}

// Define the structure of a budget category item as received from the backend
interface BudgetCategoryItem {
  category: string;
  originalValue: number;
  spentAmountSoFar: number;
  transactionHistory: { [date: string]: TransactionEntry[] };
}

function App() {
  // State for adding new categories
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newOriginalValue, setNewOriginalValue] = useState("");

  // State for recording spending
  const [spendCategory, setSpendCategory] = useState(""); // Selected category for spending
  const [amountSpent, setAmountSpent] = useState("");
  const [spendDescription, setSpendDescription] = useState("");

  // State for fetched budget data
  const [budgetItems, setBudgetItems] = useState<BudgetCategoryItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // State for selected category in the overview dropdown
  const [selectedCategoryDetails, setSelectedCategoryDetails] =
    useState<BudgetCategoryItem | null>(null);

  // Base URL for your Ktor backend
  const API_BASE_URL = "http://localhost:8080";

  // Function to fetch all budget categories from the backend
  const fetchBudgetItems = async () => {
    setErrorMessage(null);
    try {
      const response = await fetch(`${API_BASE_URL}/budget/getAllCategories`);
      if (response.ok) {
        const data: BudgetCategoryItem[] = await response.json();
        setBudgetItems(data);
        // If a category was previously selected, try to re-select its updated details
        if (selectedCategoryDetails) {
          const updatedDetails = data.find(
            (item) => item.category === selectedCategoryDetails.category
          );
          setSelectedCategoryDetails(updatedDetails || null);
        }
      } else {
        const errorText = await response.text();
        setErrorMessage(`Failed to fetch budget items: ${errorText}`);
      }
    } catch (error) {
      console.error("Error fetching budget items:", error);
      setErrorMessage("Network error or server unavailable.");
    }
  };

  // Fetch budget items on component mount and whenever a save/spend operation completes
  useEffect(() => {
    fetchBudgetItems();
  }, []);

  // Handler for adding a new budget category
  const handleAddCategory = async () => {
    setErrorMessage(null);
    if (!newCategoryName || !newOriginalValue) {
      setErrorMessage("Please enter both category name and original value.");
      return;
    }
    const originalValueNum = parseFloat(newOriginalValue);
    if (isNaN(originalValueNum)) {
      setErrorMessage("Original value must be a number.");
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/budget/addCategory`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          categoryName: newCategoryName,
          originalValue: originalValueNum,
        }),
      });

      if (response.ok) {
        setErrorMessage("Budget category saved successfully!");
        setNewCategoryName("");
        setNewOriginalValue("");
        fetchBudgetItems(); // Re-fetch to update the list
      } else {
        const errorText = await response.text();
        setErrorMessage(`Failed to save budget category: ${errorText}`);
      }
    } catch (error) {
      console.error("Error adding category:", error);
      setErrorMessage("Network error or server unavailable.");
    }
  };

  // Handler for recording spending
  const handleRecordSpend = async () => {
    setErrorMessage(null);
    if (!spendCategory || !amountSpent || !spendDescription) {
      setErrorMessage(
        "Please select a category and enter amount spent and description."
      );
      return;
    }
    const amountSpentNum = parseFloat(amountSpent);
    if (isNaN(amountSpentNum)) {
      setErrorMessage("Amount spent must be a number.");
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/budget/recordSpend`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          categoryName: spendCategory,
          amountSpent: amountSpentNum,
          description: spendDescription,
        }),
      });

      if (response.ok) {
        setErrorMessage("Spend recorded successfully!");
        setAmountSpent("");
        setSpendDescription("");
        fetchBudgetItems(); // Re-fetch to update the list and totals
      } else {
        const errorText = await response.text();
        setErrorMessage(`Failed to record spend: ${errorText}`);
      }
    } catch (error) {
      console.error("Error recording spend:", error);
      setErrorMessage("Network error or server unavailable.");
    }
  };

  // Handler for deleting a budget category
  const handleDeleteCategory = async (categoryToDelete: string) => {
    setErrorMessage(null);
    if (
      !window.confirm(
        `Are you sure you want to delete the category "${categoryToDelete}"?`
      )
    ) {
      return; // User cancelled the deletion
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/budget/deleteCategory/${categoryToDelete}`,
        {
          method: "DELETE",
        }
      );

      if (response.ok) {
        setErrorMessage(`Category "${categoryToDelete}" deleted successfully!`);
        fetchBudgetItems(); // Re-fetch to update the list
        // If the deleted category was the one currently selected, clear the details view
        if (selectedCategoryDetails?.category === categoryToDelete) {
          setSelectedCategoryDetails(null);
        }
      } else {
        const errorText = await response.text();
        setErrorMessage(
          `Failed to delete category "${categoryToDelete}": ${errorText}`
        );
      }
    } catch (error) {
      console.error("Error deleting category:", error);
      setErrorMessage("Network error or server unavailable.");
    }
  };

  // Handler for dropdown selection change
  const handleCategorySelectChange = (
    e: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const selectedCatName = e.target.value;
    const details =
      budgetItems.find((item) => item.category === selectedCatName) || null;
    setSelectedCategoryDetails(details);
  };

  return (
    <div className="min-h-screen w-screen bg-gradient-to-br from-blue-950 via-blue-800 to-blue-600 text-white p-4 font-sans">
      <div className="max-w-3xl mx-auto bg-gray-800 rounded-xl shadow-lg p-8">
        <h1 className="text-3xl font-bold mb-6 text-center text-blue-300">
          Budget Planner
        </h1>

        {errorMessage && (
          <p className="text-red-400 bg-red-900 p-3 rounded-md text-center mb-6 border border-red-700">
            {errorMessage}
          </p>
        )}

        {/* Section for adding new budget categories */}
        <div className="mb-8 p-6 bg-gray-700 rounded-lg shadow-md">
          <h2 className="text-2xl font-semibold mb-4 text-blue-200">
            Add/Update Budget Category
          </h2>
          <div className="mb-4">
            <label
              htmlFor="newCategoryName"
              className="block font-medium mb-1 text-gray-300"
            >
              Category Name:
            </label>
            <input
              type="text"
              id="newCategoryName"
              value={newCategoryName}
              onChange={(e) => setNewCategoryName(e.target.value)}
              className="w-full border border-gray-600 rounded-lg px-4 py-2 bg-gray-900 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., Groceries, Rent"
            />
          </div>
          <div className="mb-6">
            <label
              htmlFor="newOriginalValue"
              className="block font-medium mb-1 text-gray-300"
            >
              Original Budget Value:
            </label>
            <input
              type="number"
              id="newOriginalValue"
              value={newOriginalValue}
              onChange={(e) => setNewOriginalValue(e.target.value)}
              className="w-full border border-gray-600 rounded-lg px-4 py-2 bg-gray-900 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., 500.00"
            />
          </div>
          <button
            onClick={handleAddCategory}
            className="w-full bg-blue-600 text-white font-bold py-3 px-4 rounded-lg hover:bg-blue-700 transition duration-300 shadow-md"
          >
            Add/Update Category
          </button>
        </div>

        {/* Section for recording spending */}
        <div className="mb-8 p-6 bg-gray-700 rounded-lg shadow-md">
          <h2 className="text-2xl font-semibold mb-4 text-blue-200">
            Record Spending
          </h2>
          <div className="mb-4">
            <label
              htmlFor="spendCategory"
              className="block font-medium mb-1 text-gray-300"
            >
              Select Category:
            </label>
            <select
              id="spendCategory"
              value={spendCategory}
              onChange={(e) => setSpendCategory(e.target.value)}
              className="w-full border border-gray-600 rounded-lg px-4 py-2 bg-gray-900 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Select a category --</option>
              {budgetItems.map((item) => (
                <option key={item.category} value={item.category}>
                  {item.category}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-4">
            <label
              htmlFor="amountSpent"
              className="block font-medium mb-1 text-gray-300"
            >
              Amount Spent:
            </label>
            <input
              type="number"
              id="amountSpent"
              value={amountSpent}
              onChange={(e) => setAmountSpent(e.target.value)}
              className="w-full border border-gray-600 rounded-lg px-4 py-2 bg-gray-900 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., 25.50"
            />
          </div>
          <div className="mb-6">
            <label
              htmlFor="spendDescription"
              className="block font-medium mb-1 text-gray-300"
            >
              Description:
            </label>
            <input
              type="text"
              id="spendDescription"
              value={spendDescription}
              onChange={(e) => setSpendDescription(e.target.value)}
              className="w-full border border-gray-600 rounded-lg px-4 py-2 bg-gray-900 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., Coffee, Groceries"
            />
          </div>
          <button
            onClick={handleRecordSpend}
            className="w-full bg-green-600 text-white font-bold py-3 px-4 rounded-lg hover:bg-green-700 transition duration-300 shadow-md"
          >
            Record Spend
          </button>
        </div>

        {/* Section for Budget Overview and Transaction History */}
        <div className="p-6 bg-gray-700 rounded-lg shadow-md">
          <h2 className="text-2xl font-semibold mb-4 text-blue-200">
            Budget Overview
          </h2>

          {budgetItems.length === 0 ? (
            <p className="text-gray-400 text-center">
              No budget categories added yet. Add one above!
            </p>
          ) : (
            <>
              {/* Dropdown for selecting a category to view details */}
              <div className="mb-4">
                <label
                  htmlFor="categorySelect"
                  className="block font-medium mb-1 text-gray-300"
                >
                  View Details for Category:
                </label>
                <select
                  id="categorySelect"
                  value={selectedCategoryDetails?.category || ""}
                  onChange={handleCategorySelectChange}
                  className="w-full border border-gray-600 rounded-lg px-4 py-2 bg-gray-900 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">-- Select a category --</option>
                  {budgetItems.map((item) => (
                    <option key={item.category} value={item.category}>
                      {item.category}
                    </option>
                  ))}
                </select>
              </div>

              {selectedCategoryDetails && (
                <div className="mt-4 p-4 bg-gray-900 rounded-lg border border-gray-600">
                  <h3 className="text-xl font-semibold mb-2 text-blue-300">
                    {selectedCategoryDetails.category}
                  </h3>
                  <p className="text-gray-300">
                    Original Budget:{" "}
                    <span className="font-bold text-green-400">
                      ${selectedCategoryDetails.originalValue.toFixed(2)}
                    </span>
                  </p>
                  <p className="text-gray-300">
                    Spent So Far:{" "}
                    <span className="font-bold text-red-400">
                      ${selectedCategoryDetails.spentAmountSoFar.toFixed(2)}
                    </span>
                  </p>
                  <p className="text-gray-300">
                    Remaining:{" "}
                    <span className="font-bold text-yellow-400">
                      $
                      {(
                        selectedCategoryDetails.originalValue -
                        selectedCategoryDetails.spentAmountSoFar
                      ).toFixed(2)}
                    </span>
                  </p>

                  <h4 className="text-lg font-semibold mt-4 mb-2 text-blue-300">
                    Transaction History:
                  </h4>
                  {Object.keys(selectedCategoryDetails.transactionHistory)
                    .length === 0 ? (
                    <p className="text-gray-400">
                      No transactions recorded for this category yet.
                    </p>
                  ) : (
                    <ul className="divide-y divide-gray-600">
                      {Object.entries(
                        selectedCategoryDetails.transactionHistory
                      )
                        .sort(([dateA], [dateB]) => dateB.localeCompare(dateA)) // Sort dates, newest first
                        .map(([date, transactionsForDate]) => (
                          <li key={date} className="py-2">
                            <p className="font-bold text-gray-200 mb-1">
                              {date}:
                            </p>
                            <ul className="list-disc list-inside ml-4">
                              {transactionsForDate.map((transaction, index) => (
                                <li
                                  key={`${date}-${index}`}
                                  className="text-gray-300"
                                >
                                  ${transaction.amount.toFixed(2)} -{" "}
                                  {transaction.description}
                                </li>
                              ))}
                            </ul>
                          </li>
                        ))}
                    </ul>
                  )}
                </div>
              )}

              <h3 className="text-lg font-semibold mt-6 mb-2 text-blue-300">
                All Categories Summary:
              </h3>
              <ul className="divide-y divide-gray-600 rounded-lg border border-gray-600">
                {budgetItems.map((item) => (
                  <li
                    key={item.category}
                    className="flex justify-between items-center p-3 hover:bg-gray-900"
                  >
                    <span className="font-medium text-gray-200">
                      {item.category}:
                    </span>
                    <span className="text-gray-300">
                      ${item.spentAmountSoFar.toFixed(2)} / $
                      {item.originalValue.toFixed(2)} (Remaining:{" "}
                      <span className="text-yellow-400">
                        $
                        {(item.originalValue - item.spentAmountSoFar).toFixed(
                          2
                        )}
                      </span>
                      )
                    </span>
                    <button
                      onClick={() => handleDeleteCategory(item.category)}
                      className="ml-4 px-3 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 transition duration-200 text-sm"
                    >
                      Delete
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
