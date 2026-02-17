import express from "express";
import { protect } from "../../middleware/auth.middleware.js";
import { authorize } from "../../middleware/permission.middleware.js";
import { PERMISSIONS } from "../../config/permissions.matrix.js";
import { getSellerDashboard } from "./seller.controller.js";

const router = express.Router();

router.get(
  "/dashboard",
  protect,
  authorize(PERMISSIONS.VIEW_SELLER_DASHBOARD),
  getSellerDashboard,
);

export default router;
