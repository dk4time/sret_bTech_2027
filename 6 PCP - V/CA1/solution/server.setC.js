import express from "express";
import { getToken, getDataset } from "./tokenService.js";

const app = express();
const PORT = process.env.PORT || 5050;

app.use(express.json());

let employees = [];

/* ===============================
   Load Data Once (Caching)
=============================== */
const startServer = async () => {
  try {
    const response = await getToken();
    const { token, dataUrl } = response;
    const data = await getDataset(token, dataUrl);
    employees = data.employees;
    console.log("Dataset C loaded successfully");
    app.listen(PORT, () => {
      console.log(`Student SET C running on port ${PORT}`);
    });
  } catch (err) {
    console.log("Error loading data:", err.message);
  }
};

/* ===============================
   SET C Endpoints
=============================== */

/* Q1 – Highest Paid Employee
   GET /employees/highest-salary
*/
app.get("/employees/highest-salary", (req, res) => {
  if (!employees.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const highest = employees.reduce((max, emp) => {
    const total = emp.salary.basic + emp.salary.bonus;
    const maxTotal = max.salary.basic + max.salary.bonus;
    return total > maxTotal ? emp : max;
  });

  const totalSalary = highest.salary.basic + highest.salary.bonus;

  res.json({
    id: highest.id,
    name: highest.name,
    department: highest.department,
    role: highest.role,
    totalSalary,
  });
});

/* Q3 – Filter by Department
   GET /employees/filter?department=VALUE
*/
app.get("/employees/filter", (req, res) => {
  if (!employees.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const { department } = req.query;

  if (!department)
    return res.status(400).json({
      message: "department query parameter is required",
    });

  const filtered = employees.filter(
    (e) => e.department.toLowerCase() === department.toLowerCase(),
  );

  // Return minimal structure as per question paper
  const result = filtered.map((e) => ({
    id: e.id,
    name: e.name,
    department: e.department,
    role: e.role,
  }));

  res.json(result);
});

/* Q4 – Senior Employees
   GET /employees/senior/:years
*/
app.get("/employees/senior/:years", (req, res) => {
  if (!employees.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const years = Number(req.params.years);

  if (isNaN(years))
    return res.status(400).json({
      message: "Years must be a number",
    });

  const filtered = employees.filter((e) => e.experience >= years);

  const result = filtered.map((e) => ({
    id: e.id,
    name: e.name,
    experience: e.experience,
    department: e.department,
  }));

  res.json(result);
});

/* Q5 – Experience & Performance Analysis
   GET /employees/analysis/summary
*/
app.get("/employees/analysis/summary", (req, res) => {
  if (!employees.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const summary = employees.reduce(
    (acc, emp) => {
      acc.totalExperience += emp.experience;
      if (emp.performanceRating >= 4.5) acc.highPerformersCount++;
      if (emp.status === "Active") acc.activeEmployeesCount++;
      return acc;
    },
    {
      totalExperience: 0,
      highPerformersCount: 0,
      activeEmployeesCount: 0,
    },
  );

  const averageExperience = Number(
    (summary.totalExperience / employees.length).toFixed(2),
  );

  res.json({
    averageExperience,
    highPerformersCount: summary.highPerformersCount,
    activeEmployeesCount: summary.activeEmployeesCount,
  });
});

/* Health */
app.get("/", (req, res) => {
  res.json({ message: "SET C API Running" });
});

/* Q2 – Get Employee by ID
   GET /employees/:id
*/
app.get("/employees/:id", (req, res) => {
  if (!employees.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const id = Number(req.params.id);

  if (isNaN(id)) return res.status(400).json({ message: "Invalid ID" });

  const employee = employees.find((e) => e.id === id);

  if (!employee) return res.status(404).json({ message: "Employee not found" });

  res.json(employee);
});

/* =============================== */
startServer();
