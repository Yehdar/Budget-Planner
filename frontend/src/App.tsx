import { useState } from "react";
import { PieChart, Pie, Cell, Tooltip, Legend } from "recharts";

const COLORS = ["#8884d8", "#82ca9d", "#ffc658", "#ff7f7f", "#a28bd4"];

const categories = ["Rent", "Food", "Transport", "Entertainment", "Savings"];

function App() {
  const [budget, setBudget] = useState(1000);
  const [allocations, setAllocations] = useState(
    Array(categories.length).fill(20)
  ); // default 20% each

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
    <div style={{ padding: "2rem", fontFamily: "Arial" }}>
      <h1>Budget Planner</h1>

      <label>Total Budget ($): </label>
      <input
        type="number"
        value={budget}
        onChange={(e) => setBudget(Number(e.target.value))}
        style={{ marginBottom: "1rem" }}
      />

      {categories.map((cat, idx) => (
        <div key={cat} style={{ margin: "1rem 0" }}>
          <label>
            {cat}: {allocations[idx]}%
          </label>
          <input
            type="range"
            min={0}
            max={100}
            value={allocations[idx]}
            onChange={(e) => handleSliderChange(idx, parseInt(e.target.value))}
          />
        </div>
      ))}

      <p>
        Total Allocated: {totalPercent}%{" "}
        {totalPercent !== 100 && "(⚠️ should total 100%)"}
      </p>

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
  );
}

export default App;
