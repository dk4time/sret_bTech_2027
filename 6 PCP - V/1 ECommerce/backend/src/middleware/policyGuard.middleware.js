import PlatformPolicy from "../modules/governance/models/platformPolicy.model.js";
import AppError from "../utils/appError.util.js";

const requireActivePolicy = async (req, res, next) => {
  const policy = await PlatformPolicy.findOne({ isActive: true });

  if (!policy) {
    return next(
      new AppError(
        "Platform not initialized. Active policy missing.",
        503,
        "PLATFORM_NOT_INITIALIZED",
      ),
    );
  }

  req.platformPolicy = policy; // attach for downstream usage
  next();
};

export default requireActivePolicy;
