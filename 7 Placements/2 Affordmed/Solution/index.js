import express from "express";
import dotenv from "dotenv";

import maintenanceRoutes from "./backend/vehicle-maintenance/routes/maintenance.routes.js";

import { errorHandler } from "./backend/vehicle-maintenance/middleware/error.middleware.js";

dotenv.config();

const app = express();

app.use(express.json());

app.use("/api/v1/maintenance", maintenanceRoutes);

app.get("/", (req, res) => {
  res.json({
    success: true,

    message: "Affordmed Round 1 API",
  });
});

app.use(errorHandler);

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on ${PORT}`);
});
