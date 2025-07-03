import { useState } from "react";
import { PieChart, Pie, Cell, Tooltip, Legend } from "recharts";

const COLORS = ["#111", "#F5D34F", "#9AE6B4", "#A0AEC0", "#ECC94B"];
const categories = ["Rent", "Food", "Transport", "Entertainment", "Savings"];

function App() {
  const [budget, setBudget] = useState(1000);
  const [allocations, setAllocations] = useState(
    Array(categories.length).fill(20)
  );

  const handleSliderChange = (index: number, value: number) => {
    const newAlloc = [...allocations];
    newAlloc[index] = value;
    setAllocations(newAlloc);
  };

  const pieData = categories.map((name, idx) => ({
    name,
    value: parseFloat(((allocations[idx] / 100) * budget).toFixed(2)),
  }));

  const totalPercent = allocations.reduce((a, b) => a + b, 0);

  return (
    <div className="min-h-screen bg-gradient-to-br from-black via-white to-yellow-300 text-black p-6">
      <div className="max-w-3xl mx-auto bg-white rounded-xl shadow-md p-8">
        <h1 className="text-3xl font-bold mb-4">Budget Planner</h1>
        <div className="mb-6">
          <label className="block font-medium text-lg mb-1">
            Total Budget ($)
          </label>
          <input
            type="number"
            value={budget}
            onChange={(e) => setBudget(Number(e.target.value))}
            className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-gold"
          />
        </div>

        {categories.map((cat, idx) => (
          <div key={cat} className="mb-5">
            <div className="flex justify-between mb-1">
              <label className="font-medium">{cat}</label>
              <span className="text-sm text-gray-600">{allocations[idx]}%</span>
            </div>
            <input
              type="range"
              min={0}
              max={100}
              value={allocations[idx]}
              onChange={(e) =>
                handleSliderChange(idx, parseInt(e.target.value))
              }
              className="w-full accent-gold"
            />
          </div>
        ))}

        <p
          className={`text-sm mb-4 ${
            totalPercent !== 100 ? "text-red-500" : "text-green-600"
          }`}
        >
          Total Allocated: {totalPercent}%{" "}
          {totalPercent !== 100 && "⚠️ should total 100%"}
        </p>

        <div className="flex justify-center mt-8">
          <PieChart width={400} height={300}>
            <Pie
              data={pieData}
              cx="50%"
              cy="50%"
              label
              outerRadius={100}
              dataKey="value"
            >
              {pieData.map((_, index) => (
                <Cell key={index} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </div>
      </div>
    </div>
  );
}

export default App;
