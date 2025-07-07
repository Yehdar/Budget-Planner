export interface AddBudgetCategoryRequest {
  categoryName: string;
  originalValue: number;
}

export interface RecordSpendRequest {
  categoryName: string;
  amountSpent: number;
  description: string;
}

export interface TransactionEntry {
  amount: number;
  description: string;
}

export interface BudgetCategoryItem {
  category: string;
  originalValue: number;
  spentAmountSoFar: number;
  transactionHistory: { [date: string]: TransactionEntry[] };
}
