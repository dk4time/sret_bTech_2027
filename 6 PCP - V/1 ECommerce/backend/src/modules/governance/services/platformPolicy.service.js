import PlatformPolicy from "../models/platformPolicy.model.js";
import AppError from "../../../utils/appError.util.js";

export const createPolicy = async (data, adminId) => {
  const lastPolicy = await PlatformPolicy.findOne().sort({ version: -1 });

  const newVersion = lastPolicy ? lastPolicy.version + 1 : 1;

  // Deactivate existing active policy
  await PlatformPolicy.updateMany({ isActive: true }, { isActive: false });

  const newPolicy = await PlatformPolicy.create({
    ...data,
    version: newVersion,
    isActive: true,
    createdBy: adminId,
  });

  return newPolicy;
};

export const getActivePolicy = async () => {
  const policy = await PlatformPolicy.findOne({ isActive: true });

  if (!policy) {
    throw new AppError(
      "Platform policy not initialized",
      400,
      "POLICY_NOT_INITIALIZED",
    );
  }

  return policy;
};

export const getAllPolicies = async () => {
  return await PlatformPolicy.find().sort({ version: -1 });
};
