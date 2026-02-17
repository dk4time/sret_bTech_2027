import mongoose from "mongoose";

const sellerApprovalLogSchema = new mongoose.Schema(
  {
    sellerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Seller",
      required: true,
    },

    previousStatus: {
      type: String,
      enum: ["PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED", "SUSPENDED"],
      required: true,
    },

    newStatus: {
      type: String,
      enum: ["PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED", "SUSPENDED"],
      required: true,
    },

    changedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    reason: {
      type: String,
    },

    notes: {
      type: String,
    },
  },
  { timestamps: true },
);

export default mongoose.model("SellerApprovalLog", sellerApprovalLogSchema);
