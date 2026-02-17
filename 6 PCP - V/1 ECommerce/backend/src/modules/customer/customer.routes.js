import express from "express";
import { protect } from "../../middleware/auth.middleware.js";
import { authorize } from "../../middleware/permission.middleware.js";
import { PERMISSIONS } from "../../config/permissions.matrix.js";
import { getCustomerDashboard } from "./customer.controller.js";

const router = express.Router();

router.get(
  "/dashboard",
  protect,
  authorize(PERMISSIONS.VIEW_CUSTOMER_DASHBOARD),
  getCustomerDashboard,
);

export default router;
