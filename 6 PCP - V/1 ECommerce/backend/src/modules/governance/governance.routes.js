import express from "express";
import { protect } from "../../middleware/auth.middleware.js";
import { authorize } from "../../middleware/permission.middleware.js";
import { PERMISSIONS } from "../../config/permissions.matrix.js";

import * as platformPolicyController from "./controllers/platformPolicy.controller.js";
import * as subscriptionPlanController from "./controllers/subscriptionPlan.controller.js";
// import * as sellerModerationController from "./controllers/sellerModeration.controller.js";
// import * as productModerationController from "./controllers/productModeration.controller.js";

const router = express.Router();

/* Sample Admin Dashboard */
router.get(
  "/dashboard",
  protect,
  authorize(PERMISSIONS.VIEW_ADMIN_DASHBOARD),
  platformPolicyController.getAdminDashboard,
);

// Create new policy
router.post(
  "/policy",
  protect,
  authorize(PERMISSIONS.MANAGE_PLATFORM_POLICY),
  platformPolicyController.createPolicy,
);

// Get active policy
router.get(
  "/policy/active",
  protect,
  authorize(PERMISSIONS.VIEW_PLATFORM_POLICY),
  platformPolicyController.getActivePolicy,
);

// Get all policy versions
router.get(
  "/policy",
  protect,
  authorize(PERMISSIONS.VIEW_PLATFORM_POLICY),
  platformPolicyController.getAllPolicies,
);

// Subscription Plan Routes

router.post(
  "/subscription",
  protect,
  authorize(PERMISSIONS.MANAGE_SUBSCRIPTION),
  subscriptionPlanController.createPlan,
);

router.get(
  "/subscription",
  protect,
  authorize(PERMISSIONS.VIEW_SUBSCRIPTION),
  subscriptionPlanController.getPlans,
);

router.get(
  "/subscription/:name",
  protect,
  authorize(PERMISSIONS.VIEW_SUBSCRIPTION),
  subscriptionPlanController.getPlanByName,
);

router.patch(
  "/subscription/:id/deactivate",
  protect,
  authorize(PERMISSIONS.MANAGE_SUBSCRIPTION),
  subscriptionPlanController.deactivatePlan,
);

export default router;
