import mongoose from "mongoose";
import { ENV } from "./env.js";

const connectDB = async () => {
  try {
    await mongoose.connect(ENV.MONGO_URI);
    console.log("MongoDB Connected");
  } catch (error) {
    console.error("Database connection failed:", error);
    process.exit(1);
  }
};

export default connectDB;
