import express from "express";
import { getToken, getDataset } from "./tokenService.js";

const app = express();
const PORT = process.env.PORT || 5050;

app.use(express.json());

let students = [];

/* ===============================
   Load Data Once (Caching)
=============================== */
const startServer = async () => {
  try {
    const response = await getToken();
    const { token, dataUrl } = response;
    const data = await getDataset(token, dataUrl);
    students = data.students;
    console.log("Dataset F loaded successfully");
    app.listen(PORT, () => {
      console.log(`Student SET F running on port ${PORT}`);
    });
  } catch (err) {
    console.log("Error loading data:", err.message);
  }
};

/* ===============================
   SET F Endpoints
=============================== */

// Q1 – Highest Total Marks
app.get("/students/topper", (req, res) => {
  if (!students.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const topper = students.reduce((max, student) => {
    const total =
      student.marks.dsa + student.marks.mern + student.marks.aptitude;

    const maxTotal = max.marks.dsa + max.marks.mern + max.marks.aptitude;

    return total > maxTotal ? student : max;
  });

  const totalMarks =
    topper.marks.dsa + topper.marks.mern + topper.marks.aptitude;

  res.json({
    id: topper.id,
    name: topper.name,
    gender: topper.gender,
    totalMarks,
  });
});

// Q3 – Filter by MERN Marks
app.get("/students/filter", (req, res) => {
  if (!students.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const { minMern } = req.query;

  if (!minMern)
    return res.status(400).json({
      message: "minMern query parameter is required",
    });

  const value = Number(minMern);

  if (isNaN(value))
    return res.status(400).json({
      message: "minMern must be a number",
    });

  const filtered = students.filter((s) => s.marks.mern >= value);

  res.json(filtered);
});

// Q4 – Gender-wise Top Scorer
app.get("/students/top/:gender", (req, res) => {
  if (!students.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const gender = req.params.gender.toLowerCase();

  if (gender !== "male" && gender !== "female")
    return res.status(400).json({
      message: "Invalid gender provided",
    });

  const filtered = students.filter((s) => s.gender.toLowerCase() === gender);

  if (!filtered.length)
    return res.status(404).json({
      message: "No students found",
    });

  const topper = filtered.reduce((max, student) => {
    const total =
      student.marks.dsa + student.marks.mern + student.marks.aptitude;

    const maxTotal = max.marks.dsa + max.marks.mern + max.marks.aptitude;

    return total > maxTotal ? student : max;
  });

  const totalMarks =
    topper.marks.dsa + topper.marks.mern + topper.marks.aptitude;

  res.json({
    gender: topper.gender,
    id: topper.id,
    name: topper.name,
    totalMarks,
  });
});

// Q5 – Attendance Analysis
app.get("/students/attendance/average", (req, res) => {
  if (!students.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const totalAttendance = students.reduce((sum, s) => sum + s.attendance, 0);

  const above90Count = students.filter((s) => s.attendance > 90).length;

  const averageAttendance = Number(
    (totalAttendance / students.length).toFixed(2),
  );

  res.json({
    averageAttendance,
    above90Count,
  });
});

// Q2 – Get Student By ID
app.get("/students/:id", (req, res) => {
  if (!students.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const id = Number(req.params.id);

  if (isNaN(id)) return res.status(400).json({ message: "Invalid ID" });

  const student = students.find((s) => s.id === id);

  if (!student) return res.status(404).json({ message: "Student not found" });

  res.json(student);
});

// Health
app.get("/", (req, res) => {
  res.json({ message: "SET F API Running" });
});

/* =============================== */

startServer();
