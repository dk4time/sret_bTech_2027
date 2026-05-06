import express from "express";
import dotenv from "dotenv";

import connectDB from "./config/db.js";
import cveRoutes from "./routes/cveRoutes.js";

dotenv.config();

const app = express();

app.use(express.json());

// DB Connection
connectDB();

// Routes
app.use("/api/cves", cveRoutes);

app.get("/", (req, res) => {
  res.send("API Running");
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
