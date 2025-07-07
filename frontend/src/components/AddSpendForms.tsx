import React, { useState } from "react";
import type {
  AddBudgetCategoryRequest,
  RecordSpendRequest,
} from "../models/types";

interface AddSpendFormsProps {
  onAddCategory: (request: AddBudgetCategoryRequest) => Promise<void>;
  onRecordSpend: (request: RecordSpendRequest) => Promise<void>;
  isLoading: boolean;
}

const AddSpendForms: React.FC<AddSpendFormsProps> = ({
  onAddCategory,
  onRecordSpend,
  isLoading,
}) => {
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newCategoryValue, setNewCategoryValue] = useState<number | "">("");
  const [spendCategoryName, setSpendCategoryName] = useState("");
  const [spendAmount, setSpendAmount] = useState<number | "">("");
  const [spendDescription, setSpendDescription] = useState("");

  const handleAddCategorySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newCategoryName && newCategoryValue !== "") {
      await onAddCategory({
        categoryName: newCategoryName,
        originalValue: Number(newCategoryValue),
      });
      setNewCategoryName("");
      setNewCategoryValue("");
    }
  };

  const handleRecordSpendSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (spendCategoryName && spendAmount !== "" && spendDescription) {
      await onRecordSpend({
        categoryName: spendCategoryName,
        amountSpent: Number(spendAmount),
        description: spendDescription,
      });
      setSpendCategoryName("");
      setSpendAmount("");
      setSpendDescription("");
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-md mb-8">
      <h2 className="text-2xl font-bold text-gray-800 mb-6">Manage Budget</h2>

      {/* Add Budget Category Form */}
      <form
        onSubmit={handleAddCategorySubmit}
        className="mb-8 p-4 border border-gray-200 rounded-lg"
      >
        <h3 className="text-xl font-semibold text-gray-700 mb-4">
          Add/Update Budget Category
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label
              htmlFor="categoryName"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Category Name
            </label>
            <input
              type="text"
              id="categoryName"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
              value={newCategoryName}
              onChange={(e) => setNewCategoryName(e.target.value)}
              placeholder="e.g., Groceries"
              required
            />
          </div>
          <div>
            <label
              htmlFor="originalValue"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Budget Value
            </label>
            <input
              type="number"
              id="originalValue"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
              value={newCategoryValue}
              onChange={(e) => setNewCategoryValue(Number(e.target.value))}
              placeholder="e.g., 500.00"
              step="0.01"
              required
            />
          </div>
        </div>
        <button
          type="submit"
          className="mt-6 w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 transition duration-200 disabled:opacity-50"
          disabled={isLoading}
        >
          {isLoading ? "Processing..." : "Add/Update Category"}
        </button>
      </form>

      {/* Record Spend Form */}
      <form
        onSubmit={handleRecordSpendSubmit}
        className="p-4 border border-gray-200 rounded-lg"
      >
        <h3 className="text-xl font-semibold text-gray-700 mb-4">
          Record Spending
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <label
              htmlFor="spendCategory"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Category
            </label>
            <input
              type="text"
              id="spendCategory"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
              value={spendCategoryName}
              onChange={(e) => setSpendCategoryName(e.target.value)}
              placeholder="e.g., Groceries"
              required
            />
          </div>
          <div>
            <label
              htmlFor="spendAmount"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Amount Spent
            </label>
            <input
              type="number"
              id="spendAmount"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
              value={spendAmount}
              onChange={(e) => setSpendAmount(Number(e.target.value))}
              placeholder="e.g., 25.50"
              step="0.01"
              required
            />
          </div>
        </div>
        <div>
          <label
            htmlFor="spendDescription"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Description
          </label>
          <input
            type="text"
            id="spendDescription"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
            value={spendDescription}
            onChange={(e) => setSpendDescription(e.target.value)}
            placeholder="e.g., Weekly shopping at Walmart"
            required
          />
        </div>
        <button
          type="submit"
          className="mt-6 w-full bg-green-600 text-white py-2 px-4 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-opacity-50 transition duration-200 disabled:opacity-50"
          disabled={isLoading}
        >
          {isLoading ? "Processing..." : "Record Spend"}
        </button>
      </form>
    </div>
  );
};

export default AddSpendForms;
