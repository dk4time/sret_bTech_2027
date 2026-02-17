import express from "express";
import * as authController from "./auth.controller.js";
import { protect } from "../../middleware/auth.middleware.js";

const router = express.Router();

router.post("/register", authController.register);
router.post("/login", authController.login);
router.post("/refresh", authController.refresh);
router.get("/me", protect, authController.getMe);
router.post("/logout", authController.logout);

export default router;
