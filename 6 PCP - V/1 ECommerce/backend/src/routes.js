import express from "express";

import authRoutes from "./modules/identity/auth.routes.js";
import governanceRoutes from "./modules/governance/governance.routes.js";
import customerRoutes from "./modules/customer/customer.routes.js";
import sellerRoutes from "./modules/seller/seller.routes.js";

const router = express.Router();

router.use("/auth", authRoutes);
router.use("/governance", governanceRoutes);
router.use("/customer", customerRoutes);
router.use("/seller", sellerRoutes);

export default router;
