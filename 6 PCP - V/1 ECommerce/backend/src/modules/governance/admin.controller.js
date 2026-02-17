import { successResponse } from "../../utils/response.util.js";

export const getAdminDashboard = async (req, res) => {
  successResponse(
    res,
    {
      id: req.user._id,
      email: req.user.email,
      roles: req.user.roles,
    },
    "Admin Dashboard",
  );
  //
  // res.status(200).json({
  //   message: "Admin Dashboard",
  //   user: {
  //     id: req.user._id,
  //     email: req.user.email,
  //     roles: req.user.roles,
  //   },
  // });
};
