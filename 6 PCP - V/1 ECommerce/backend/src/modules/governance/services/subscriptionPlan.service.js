import SubscriptionPlan from "../models/subscriptionPlan.model.js";
import AppError from "../../../utils/appError.util.js";

export const createPlan = async (data, adminId) => {
  const { name } = data;

  const lastVersion = await SubscriptionPlan.findOne({ name }).sort({
    version: -1,
  });

  const newVersion = lastVersion ? lastVersion.version + 1 : 1;

  const newPlan = await SubscriptionPlan.create({
    ...data,
    version: newVersion,
    createdBy: adminId,
  });

  return newPlan;
};

export const getAllPlans = async () => {
  return await SubscriptionPlan.find().sort({ name: 1, version: -1 });
};

export const getLatestActivePlan = async (name) => {
  const plan = await SubscriptionPlan.findOne({
    name,
    isActive: true,
  }).sort({ version: -1 });

  if (!plan) {
    throw new AppError("Plan not found", 404, "PLAN_NOT_FOUND");
  }

  return plan;
};

export const deactivatePlan = async (planId) => {
  const plan = await SubscriptionPlan.findById(planId);

  if (!plan) {
    throw new AppError("Plan not found", 404);
  }

  plan.isActive = false;
  await plan.save();

  return plan;
};
