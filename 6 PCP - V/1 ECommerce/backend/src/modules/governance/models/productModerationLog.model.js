import mongoose from "mongoose";

const productModerationLogSchema = new mongoose.Schema(
  {
    productId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Product",
      required: true,
    },

    sellerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Seller",
      required: true,
    },

    previousStatus: {
      type: String,
      enum: [
        "DRAFT",
        "PENDING_APPROVAL",
        "APPROVED",
        "REJECTED",
        "SUSPENDED",
        "BLOCKED",
      ],
      required: true,
    },

    newStatus: {
      type: String,
      enum: [
        "DRAFT",
        "PENDING_APPROVAL",
        "APPROVED",
        "REJECTED",
        "SUSPENDED",
        "BLOCKED",
      ],
      required: true,
    },

    moderatedBy: {
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

export default mongoose.model(
  "ProductModerationLog",
  productModerationLogSchema,
);
