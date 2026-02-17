import { successResponse } from "../../utils/response.util.js";

export const getSellerDashboard = async (req, res) => {
  successResponse(
    res,
    {
      id: req.user._id,
      email: req.user.email,
      roles: req.user.roles,
    },
    "Seller Dashboard",
  );
  //
  // res.status(200).json({
  //   message: "Seller Dashboard",
  //   user: {
  //     id: req.user._id,
  //     email: req.user.email,
  //     roles: req.user.roles,
  //   },
  // });
};
