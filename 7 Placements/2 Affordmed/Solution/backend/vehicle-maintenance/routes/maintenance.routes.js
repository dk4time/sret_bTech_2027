import express from "express";
import { scheduleMaintenance } from "../controllers/maintenance.controller.js";

const router = express.Router();

router.post("/schedule", scheduleMaintenance);

export default router;
