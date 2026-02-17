import asyncHandler from "../../../utils/asyncHandler.util.js";
import { successResponse } from "../../../utils/response.util.js";
import * as platformPolicyService from "../services/platformPolicy.service.js";

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

export const createPolicy = asyncHandler(async (req, res) => {
  const policy = await platformPolicyService.createPolicy(
    req.body,
    req.user._id,
  );

  successResponse(res, policy, "Policy created", 201);
});

export const getActivePolicy = asyncHandler(async (req, res) => {
  const policy = await platformPolicyService.getActivePolicy();

  successResponse(res, policy, "Active policy fetched");
});

export const getAllPolicies = asyncHandler(async (req, res) => {
  const policies = await platformPolicyService.getAllPolicies();

  successResponse(res, policies, "All policies fetched");
});
