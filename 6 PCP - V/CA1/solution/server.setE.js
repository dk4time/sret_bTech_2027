import express from "express";
import { getToken, getDataset } from "./tokenService.js";

const app = express();
const PORT = process.env.PORT || 5050;

app.use(express.json());

let enrollments = [];

/* ===============================
   Load Data Once (Caching)
=============================== */
const startServer = async () => {
  try {
    const response = await getToken();
    const { token, dataUrl } = response;
    const data = await getDataset(token, dataUrl);
    enrollments = data.enrollments;
    console.log(data);
    console.log("Dataset E loaded successfully");
    app.listen(PORT, () => {
      console.log(`Student SET E running on port ${PORT}`);
    });
  } catch (err) {
    console.log("Error loading data:", err.message);
  }
};

/* =============================
   Q1 – Highest Attendance Student
   GET /enrollments/top-attendance
============================= */

app.get("/enrollments/top-attendance", (req, res) => {
  if (!enrollments.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const topStudent = enrollments.reduce((max, student) =>
    student.attendance > max.attendance ? student : max,
  );

  res.json({
    id: topStudent.id,
    studentName: topStudent.studentName,
    course: topStudent.course,
    attendance: topStudent.attendance,
  });
});

/* =============================
   Q3 – Filter by Department
   GET /enrollments/filter?department=value
============================= */

app.get("/enrollments/filter", (req, res) => {
  if (!enrollments.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const { department } = req.query;

  if (!department)
    return res.status(400).json({
      message: "department query parameter is required",
    });

  const filtered = enrollments.filter(
    (e) => e.department.toLowerCase() === department.toLowerCase(),
  );

  res.json(filtered);
});

/* =============================
   Q4 – Most Enrolled Course
   GET /enrollments/course/popular
============================= */

app.get("/enrollments/course/popular", (req, res) => {
  if (!enrollments.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const courseCount = {};

  enrollments.forEach((e) => {
    courseCount[e.course] = (courseCount[e.course] || 0) + 1;
  });

  let popularCourse = null;
  let max = 0;

  for (const course in courseCount) {
    if (courseCount[course] > max) {
      max = courseCount[course];
      popularCourse = course;
    }
  }

  res.json({
    course: popularCourse,
    totalEnrollments: max,
  });
});

/* =============================
   Q5 – Attendance Summary
   GET /enrollments/attendance/summary
============================= */

app.get("/enrollments/attendance/summary", (req, res) => {
  if (!enrollments.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const result = enrollments.reduce(
    (acc, student) => {
      acc.totalAttendance += student.attendance;

      if (student.attendance > 90) acc.above90Count++;

      return acc;
    },
    { totalAttendance: 0, above90Count: 0 },
  );

  const averageAttendance = (
    result.totalAttendance / enrollments.length
  ).toFixed(2);

  res.json({
    averageAttendance: Number(averageAttendance),
    above90Count: result.above90Count,
  });
});

/* =============================
   Q2 – Get Enrollment By ID
   GET /enrollments/:id
============================= */

app.get("/enrollments/:id", (req, res) => {
  if (!enrollments.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const id = Number(req.params.id);

  if (isNaN(id)) return res.status(400).json({ message: "Invalid ID" });

  const enrollment = enrollments.find((e) => e.id === id);

  if (!enrollment)
    return res.status(404).json({ message: "Enrollment not found" });

  res.json(enrollment);
});

/* Health Check */

app.get("/", (req, res) => {
  res.json({ message: "SET E API Running" });
});

/* Start server after dataset loads */
startServer();
