import mongoose from "mongoose";

const commissionLedgerSchema = new mongoose.Schema(
  {
    orderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Order",
      required: true,
    },

    sellerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Seller",
      required: true,
    },

    policyVersion: {
      type: Number,
      required: true,
    },

    grossAmount: {
      type: Number,
      required: true,
    },

    commissionType: {
      type: String,
      enum: ["PERCENTAGE", "FLAT"],
      required: true,
    },

    commissionValue: {
      type: Number,
      required: true,
    },

    commissionAmount: {
      type: Number,
      required: true,
    },

    sellerReceivable: {
      type: Number,
      required: true,
    },

    refundedCommissionAmount: {
      type: Number,
      default: 0,
    },

    settlementStatus: {
      type: String,
      enum: ["PENDING", "PARTIALLY_SETTLED", "SETTLED"],
      default: "PENDING",
    },

    settledAt: {
      type: Date,
    },
  },
  { timestamps: true },
);

export default mongoose.model("CommissionLedger", commissionLedgerSchema);
