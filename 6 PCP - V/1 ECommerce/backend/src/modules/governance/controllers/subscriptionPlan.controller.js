import asyncHandler from "../../../utils/asyncHandler.util.js";
import { successResponse } from "../../../utils/response.util.js";
import * as service from "../services/subscriptionPlan.service.js";

export const createPlan = asyncHandler(async (req, res) => {
  const plan = await service.createPlan(req.body, req.user._id);
  successResponse(res, plan, "Subscription plan created", 201);
});

export const getPlans = asyncHandler(async (req, res) => {
  const plans = await service.getAllPlans();
  successResponse(res, plans, "Subscription plans fetched");
});

export const getPlanByName = asyncHandler(async (req, res) => {
  const plan = await service.getLatestActivePlan(req.params.name);
  successResponse(res, plan, "Subscription plan fetched");
});

export const deactivatePlan = asyncHandler(async (req, res) => {
  const plan = await service.deactivatePlan(req.params.id);
  successResponse(res, plan, "Subscription plan deactivated");
});
