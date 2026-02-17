import AppError from "../utils/appError.util.js";
import { ROLE_PERMISSIONS } from "../config/permissions.matrix.js";

const authorize = (...requiredPermissions) => {
  return (req, res, next) => {
    if (!req.user) {
      return next(new AppError("Unauthorized", 401, "UNAUTHORIZED"));
    }

    const userRoles = req.user.roles || [];

    const userPermissions = userRoles.flatMap(
      (role) => ROLE_PERMISSIONS[role] || [],
    );

    const hasPermission = requiredPermissions.every((permission) =>
      userPermissions.includes(permission),
    );

    if (!hasPermission) {
      return next(new AppError("Access denied", 403, "FORBIDDEN_ACCESS"));
    }

    next();
  };
};

export { authorize };
