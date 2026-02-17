import { successResponse } from "../../utils/response.util.js";
import * as authService from "./auth.service.js";

export const register = async (req, res, next) => {
  try {
    const { user, accessToken, refreshToken } = await authService.register(
      req.body,
    );

    res.cookie("refreshToken", refreshToken, {
      httpOnly: true,
      secure: true,
      sameSite: "strict",
      maxAge: 7 * 24 * 60 * 60 * 1000,
    });

    successResponse(
      res,
      { user, accessToken },
      "User registered successfully",
      201,
    );
    // res.status(201).json({ user, accessToken });
  } catch (error) {
    next(error);
  }
};

export const login = async (req, res, next) => {
  try {
    const { accessToken, refreshToken } = await authService.login(
      req.body.email,
      req.body.password,
    );

    res.cookie("refreshToken", refreshToken, {
      httpOnly: true,
      secure: true,
      sameSite: "strict",
      maxAge: 7 * 24 * 60 * 60 * 1000,
    });

    successResponse(res, { accessToken }, "Logged in successfully");
    // res.status(200).json({ accessToken });
  } catch (error) {
    next(error);
  }
};

export const refresh = async (req, res, next) => {
  try {
    const token = req.cookies.refreshToken;
    const accessToken = await authService.refreshAccessToken(token);

    successResponse(
      res,
      { accessToken },
      "Access token refreshed successfully",
    );
    // res.status(200).json({ accessToken });
  } catch (error) {
    next(error);
  }
};

export const getMe = async (req, res) => {
  const user = req.user;

  successResponse(
    res,
    {
      id: user._id,
      email: user.email,
      phone: user.phone,
      roles: user.roles,
      isActive: user.isActive,
      isEmailVerified: user.isEmailVerified,
      lastLoginAt: user.lastLoginAt,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    },
    "User profile retrieved successfully",
  );
  //
  // res.status(200).json({
  //   id: user._id,
  //   email: user.email,
  //   phone: user.phone,
  //   roles: user.roles,
  //   isActive: user.isActive,
  //   isEmailVerified: user.isEmailVerified,
  //   lastLoginAt: user.lastLoginAt,
  //   createdAt: user.createdAt,
  //   updatedAt: user.updatedAt,
  // });
};

export const logout = async (req, res, next) => {
  try {
    if (req.user) {
      await authService.logout(req.user.id);
    }

    res.clearCookie("refreshToken");
    successResponse(res, null, "Logged out successfully");
    // res.status(200).json({ message: "Logged out successfully" });
  } catch (error) {
    next(error);
  }
};
