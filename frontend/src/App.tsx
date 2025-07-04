import { useState, useEffect } from "react";

function App() {
  const [categoryName, setCategoryName] = useState("");
  const [initialValue, setInitialValue] = useState("");
  const [budgetItems, setBudgetItems] = useState<{ [key: string]: string }>({}); // To store fetched budget items
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>(""); // For the dropdown selection

  // Base URL for your Ktor backend
  const API_BASE_URL = "http://localhost:8080"; // Make sure this matches your Ktor server port

  // Function to fetch budget items from the backend
  const fetchBudgetItems = async () => {
    setErrorMessage(null);
    try {
      const response = await fetch(`${API_BASE_URL}/budget/get`);
      if (response.ok) {
        const data = await response.json(); // Backend returns JSON object
        setBudgetItems(data);
      } else {
        const errorText = await response.text();
        setErrorMessage(`Failed to fetch budget items: ${errorText}`);
      }
    } catch (error) {
      console.error("Error fetching budget items:", error);
      setErrorMessage("Network error or server unavailable.");
    }
  };

  // Fetch budget items on component mount
  useEffect(() => {
    fetchBudgetItems();
  }, []); // Empty dependency array means this runs once on mount

  const handleSaveBudgetItem = async () => {
    setErrorMessage(null);
    if (!categoryName || !initialValue) {
      setErrorMessage("Please enter both category name and initial value.");
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/budget/save`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ categoryName, initialValue }),
      });

      if (response.ok) {
        setErrorMessage("Budget item saved successfully!");
        setCategoryName(""); // Clear input fields
        setInitialValue("");
        fetchBudgetItems(); // Re-fetch items to update the list/dropdown
      } else {
        const errorText = await response.text();
        setErrorMessage(`Failed to save budget item: ${errorText}`);
      }
    } catch (error) {
      console.error("Error saving budget item:", error);
      setErrorMessage("Network error or server unavailable.");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-black via-white to-yellow-300 text-black p-6 font-sans">
      <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-md p-8">
        <h1 className="text-3xl font-bold mb-6 text-center text-gray-800">
          Budget Planner
        </h1>

        {/* Section for adding new budget items */}
        <div className="mb-8 p-6 border border-gray-200 rounded-lg shadow-sm">
          <h2 className="text-2xl font-semibold mb-4 text-gray-700">
            Add New Budget Item
          </h2>
          <div className="mb-4">
            <label
              htmlFor="categoryName"
              className="block font-medium mb-1 text-gray-700"
            >
              Category Name:
            </label>
            <input
              type="text"
              id="categoryName"
              value={categoryName}
              onChange={(e) => setCategoryName(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-yellow-500"
              placeholder="e.g., Groceries, Rent"
            />
          </div>
          <div className="mb-6">
            <label
              htmlFor="initialValue"
              className="block font-medium mb-1 text-gray-700"
            >
              Initial Value:
            </label>
            <input
              type="text" // Keep as text to allow flexible input (e.g., "500", "20%")
              id="initialValue"
              value={initialValue}
              onChange={(e) => setInitialValue(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-yellow-500"
              placeholder="e.g., 500, 200"
            />
          </div>
          <button
            onClick={handleSaveBudgetItem}
            className="w-full bg-yellow-500 text-white font-bold py-3 px-4 rounded-lg hover:bg-yellow-600 transition duration-300 shadow-md"
          >
            Save Budget Item
          </button>
        </div>

        {errorMessage && (
          <p className="text-red-600 bg-red-100 p-3 rounded-md text-center mb-6 border border-red-200">
            {errorMessage}
          </p>
        )}

        {/* Section for displaying budget items */}
        <div className="p-6 border border-gray-200 rounded-lg shadow-sm">
          <h2 className="text-2xl font-semibold mb-4 text-gray-700">
            Your Budget Overview
          </h2>

          {Object.keys(budgetItems).length === 0 ? (
            <p className="text-gray-600 text-center">
              No budget items saved yet. Add one above!
            </p>
          ) : (
            <>
              {/* Dropdown for selecting a category */}
              <div className="mb-4">
                <label
                  htmlFor="categorySelect"
                  className="block font-medium mb-1 text-gray-700"
                >
                  Select Category:
                </label>
                <select
                  id="categorySelect"
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-yellow-500 bg-white"
                >
                  <option value="">-- Select a category --</option>
                  {Object.keys(budgetItems).map((key) => (
                    <option key={key} value={key}>
                      {key}
                    </option>
                  ))}
                </select>
              </div>

              {/* Display selected category's value */}
              {selectedCategory && (
                <div className="mt-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                  <p className="text-lg font-semibold text-gray-800">
                    {selectedCategory}:
                  </p>
                  <p className="text-xl text-green-700 font-bold">
                    {budgetItems[selectedCategory]}
                  </p>
                </div>
              )}

              {/* List of all budget items (optional, but good for overview) */}
              <h3 className="text-lg font-semibold mt-6 mb-2 text-gray-700">
                All Budget Items:
              </h3>
              <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200">
                {Object.entries(budgetItems).map(([key, value]) => (
                  <li
                    key={key}
                    className="flex justify-between items-center p-3 hover:bg-gray-50"
                  >
                    <span className="font-medium text-gray-800">{key}:</span>
                    <span className="text-gray-700">{value}</span>
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
