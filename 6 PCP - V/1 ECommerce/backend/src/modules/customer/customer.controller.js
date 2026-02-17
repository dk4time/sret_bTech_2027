import { successResponse } from "../../utils/response.util.js";

export const getCustomerDashboard = async (req, res) => {
  successResponse(
    res,
    {
      id: req.user._id,
      email: req.user.email,
      roles: req.user.roles,
    },
    "Customer Dashboard",
  );
  //
  // res.status(200).json({
  //   message: "Customer Dashboard",
  //   user: {
  //     id: req.user._id,
  //     email: req.user.email,
  //     roles: req.user.roles,
  //   },
  // });
};
