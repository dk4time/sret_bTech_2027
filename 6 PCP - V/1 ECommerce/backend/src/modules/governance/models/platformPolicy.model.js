import mongoose from "mongoose";

const commissionPolicySchema = new mongoose.Schema(
  {
    type: {
      type: String,
      enum: ["PERCENTAGE", "FLAT"],
      default: "PERCENTAGE",
    },
    value: {
      type: Number,
      required: true,
    },
  },
  { _id: false },
);

const refundPolicySchema = new mongoose.Schema(
  {
    allowedDays: {
      type: Number,
      default: 7,
    },
    partialRefundAllowed: {
      type: Boolean,
      default: true,
    },
  },
  { _id: false },
);

const deliveryPolicySchema = new mongoose.Schema(
  {
    baseCharge: {
      type: Number,
      default: 50,
    },
    freeDeliveryAbove: {
      type: Number,
      default: 1000,
    },
  },
  { _id: false },
);

const platformPolicySchema = new mongoose.Schema(
  {
    version: {
      type: Number,
      required: true,
      unique: true,
    },

    commissionPolicy: commissionPolicySchema,

    refundPolicy: refundPolicySchema,

    deliveryPolicy: deliveryPolicySchema,

    isActive: {
      type: Boolean,
      default: false,
    },

    createdBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    updatedBy: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
    },
  },
  { timestamps: true },
);

export default mongoose.model("PlatformPolicy", platformPolicySchema);
