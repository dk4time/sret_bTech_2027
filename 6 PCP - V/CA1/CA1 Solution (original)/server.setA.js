import express from "express";
import { getToken, getDataset } from "./tokenService.js";

const app = express();
const PORT = process.env.PORT || 5050;

app.use(express.json());

let patients = [];

/* ===============================
   Load Data Once (Caching)
=============================== */
const startServer = async () => {
  try {
    const response = await getToken();
    const { token, dataUrl } = response;
    const data = await getDataset(token, dataUrl);
    patients = data.patients;
    console.log("Dataset A loaded successfully");
    app.listen(PORT, () => {
      console.log(`Student SET A running on port ${PORT}`);
    });
  } catch (err) {
    console.log("Error loading data:", err.message);
  }
};

/* =============================
   Helper – Calculate Total Bill
============================= */

const calculateBill = (patient) => {
  return patient.bill.consultation + patient.bill.medicine + patient.bill.lab;
};

/* =============================
   Q1 – Highest Total Hospital Bill
   GET /patients/highest-bill
============================= */

app.get("/patients/highest-bill", (req, res) => {
  if (!patients.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const highest = patients.reduce((max, patient) => {
    return calculateBill(patient) > calculateBill(max) ? patient : max;
  });

  res.json({
    id: highest.id,
    name: highest.name,
    department: highest.department,
    totalBill: calculateBill(highest),
  });
});

/* =============================
   Q3 – Filter by Department
   GET /patients/filter?department=value
============================= */

app.get("/patients/filter", (req, res) => {
  if (!patients.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const { department } = req.query;

  if (!department)
    return res.status(400).json({
      message: "department query parameter is required",
    });

  const filtered = patients.filter(
    (p) => p.department.toLowerCase() === department.toLowerCase(),
  );

  res.json(filtered);
});

/* =============================
   Q4 – Longest Stay Patient
   GET /patients/longest-stay
============================= */

app.get("/patients/longest-stay", (req, res) => {
  if (!patients.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const longest = patients.reduce((max, patient) =>
    patient.daysAdmitted > max.daysAdmitted ? patient : max,
  );

  res.json({
    id: longest.id,
    name: longest.name,
    department: longest.department,
    daysAdmitted: longest.daysAdmitted,
  });
});

/* =============================
   Q5 – Admission Summary
   GET /patients/admission/summary
============================= */

app.get("/patients/admission/summary", (req, res) => {
  if (!patients.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const result = patients.reduce(
    (acc, patient) => {
      if (patient.status === "Admitted") acc.admittedCount++;

      if (patient.status === "Discharged") acc.dischargedCount++;

      acc.totalAge += patient.age;

      return acc;
    },
    {
      admittedCount: 0,
      dischargedCount: 0,
      totalAge: 0,
    },
  );

  const averageAge = (result.totalAge / patients.length).toFixed(2);

  res.json({
    admittedCount: result.admittedCount,
    dischargedCount: result.dischargedCount,
    averageAge: Number(averageAge),
  });
});

/* =============================
   Q2 – Get Patient By ID
   GET /patients/:id
============================= */

app.get("/patients/:id", (req, res) => {
  if (!patients.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const id = Number(req.params.id);

  if (isNaN(id)) return res.status(400).json({ message: "Invalid ID" });

  const patient = patients.find((p) => p.id === id);

  if (!patient) return res.status(404).json({ message: "Patient not found" });

  res.json(patient);
});

/* ============================= */

app.get("/", (req, res) => {
  res.json({ message: "SET E API running" });
});

/* Start server after data load */
startServer();
