// src/models/user.model.js
import mongoose from "mongoose";

const userSchema = new mongoose.Schema(
  {
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
    },

    phone: {
      type: String,
      unique: true,
      sparse: true, // allows null but unique if present
    },

    password: {
      type: String,
      required: true,
      select: false, // never return password by default
    },

    roles: {
      type: [String],
      enum: ["CUSTOMER", "SELLER", "ADMIN"],
      default: ["CUSTOMER"],
    },

    isActive: {
      type: Boolean,
      default: true,
    },

    isEmailVerified: {
      type: Boolean,
      default: false,
    },

    lastLoginAt: {
      type: Date,
    },

    loginAttempts: {
      type: Number,
      default: 0,
    },

    lockUntil: {
      type: Date,
    },

    refreshToken: {
      type: String,
    },
  },
  {
    timestamps: true, //creates createdAt and updatedAt fields automatically
  },
);

userSchema.methods.isLocked = function () {
  return !!(this.lockUntil && this.lockUntil > Date.now());
};

export default mongoose.model("User", userSchema);
