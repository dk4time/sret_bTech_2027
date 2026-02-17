import jwt from "jsonwebtoken";
import { ENV } from "../config/env.js";

export const generateAccessToken = (user) => {
  return jwt.sign({ id: user._id, roles: user.roles }, ENV.JWT_ACCESS_SECRET, {
    expiresIn: "15m",
  });
};

export const generateRefreshToken = (user) => {
  return jwt.sign({ id: user._id }, ENV.JWT_REFRESH_SECRET, {
    expiresIn: "7d",
  });
};

export const verifyRefreshToken = (token) => {
  return jwt.verify(token, ENV.JWT_REFRESH_SECRET);
};
