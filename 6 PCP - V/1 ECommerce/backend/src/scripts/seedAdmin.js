import mongoose from "mongoose";
import { ENV } from "../config/env.js";
import connectDB from "../config/db.js";
import User from "../models/user.model.js";
import { hashPassword } from "../utils/password.util.js";

const seedAdmin = async () => {
  await connectDB();

  const existingAdmin = await User.findOne({ roles: "ADMIN" });

  if (existingAdmin) {
    console.log("Admin already exists");
    process.exit();
  }

  const hashedPassword = await hashPassword("Admin@123");

  await User.create({
    email: "admin@monsta.com",
    password: hashedPassword,
    roles: ["ADMIN"],
    isEmailVerified: true,
  });

  console.log("Admin created successfully");
  process.exit();
};

seedAdmin();
