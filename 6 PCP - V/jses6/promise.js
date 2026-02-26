// server.js
// Demonstrates Promise and Async/Await using mongoose connection

import express from "express";
import mongoose from "mongoose";

const app = express();

// -------------------------------------
// 1. Using Promise (.then / .catch)
// -------------------------------------

mongoose
  .connect("YOUR_MONGODB_URL")
  .then(() => {
    console.log("Connected to MongoDB");

    app.listen(5050, () => {
      console.log("Server is running on port 5050");
    });
  })
  .catch((err) => {
    console.log("Connection Error:", err);
  });

// -------------------------------------
// 2. Using Async / Await
// -------------------------------------

const connectDB = async () => {
  try {
    await mongoose.connect("YOUR_MONGODB_URL");

    console.log("Connected to MongoDB");

    app.listen(5050, () => {
      console.log("Server is running on port 5050");
    });
  } catch (err) {
    console.log("Connection Error:", err);
  }
};

connectDB();

// -------------------------------------
// Middleware
// -------------------------------------

app.use("/", (req, res) => {
  res.json({ message: "API is Running" });
});
