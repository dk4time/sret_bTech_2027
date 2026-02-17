import AppError from "../../utils/appError.util.js";
import asyncHandler from "../../utils/asyncHandler.util.js";
import User from "./models/user.model.js";
import { hashPassword, comparePassword } from "../../utils/password.util.js";
import {
  generateAccessToken,
  generateRefreshToken,
  verifyRefreshToken,
} from "../../utils/jwt.utils.js";

const MAX_LOGIN_ATTEMPTS = 5;
const LOCK_TIME = 15 * 60 * 1000;

export const register = asyncHandler(async ({ name, email, password }) => {
  const existingUser = await User.findOne({ email });
  if (existingUser) {
    throw new AppError("User already exists");
  }

  const hashedPassword = await hashPassword(password);

  const user = await User.create({
    name,
    email,
    password: hashedPassword,
  });

  const accessToken = generateAccessToken(user);
  const refreshToken = generateRefreshToken(user);

  user.refreshToken = refreshToken;
  await user.save();

  return { user, accessToken, refreshToken };
});

export const login = asyncHandler(async (email, password) => {
  const user = await User.findOne({ email }).select("+password");

  if (!user) throw new AppError("Invalid credentials");

  if (!user.isActive) {
    throw new AppError("Account disabled");
  }

  if (user.isLocked()) {
    throw new AppError("Account locked. Try later.");
  }

  const isMatch = await comparePassword(password, user.password);

  if (!isMatch) {
    user.loginAttempts += 1;

    if (user.loginAttempts >= MAX_LOGIN_ATTEMPTS) {
      user.lockUntil = Date.now() + LOCK_TIME;
    }

    await user.save();
    throw new AppError("Invalid credentials");
  }

  // Successful login
  user.loginAttempts = 0;
  user.lockUntil = undefined;
  user.lastLoginAt = new Date();

  const accessToken = generateAccessToken(user);
  const refreshToken = generateRefreshToken(user);

  user.refreshToken = refreshToken;

  await user.save();

  return { accessToken, refreshToken };
});

export const refreshAccessToken = asyncHandler(async (token) => {
  if (!token) throw new AppError("No refresh token");

  const decoded = verifyRefreshToken(token);

  const user = await User.findById(decoded.id);
  if (!user || user.refreshToken !== token) {
    throw new AppError("Invalid refresh token");
  }

  return generateAccessToken(user);
});

export const logout = asyncHandler(async (userId) => {
  await User.findByIdAndUpdate(userId, { refreshToken: null });
});
