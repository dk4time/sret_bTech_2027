const authorizeRoles = (...allowedRoles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const hasAccess = req.user.roles.some((role) =>
      allowedRoles.includes(role),
    );

    if (!hasAccess) {
      return res.status(403).json({ message: "Access denied" });
    }

    next();
  };
};

export { authorizeRoles };
