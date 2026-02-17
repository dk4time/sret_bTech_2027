import mongoose from "mongoose";

const benefitsSchema = new mongoose.Schema(
  {
    freeDelivery: {
      type: Boolean,
      default: false,
    },
    extraDiscountPercentage: {
      type: Number,
      default: 0,
    },
    rewardMultiplier: {
      type: Number,
      default: 1,
    },
  },
  { _id: false },
);

const subscriptionPlanSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      enum: ["BASIC", "PREMIUM", "ELITE"],
      required: true,
    },

    version: {
      type: Number,
      required: true,
    },

    price: {
      type: Number,
      required: true,
    },

    durationInDays: {
      type: Number,
      required: true,
    },

    benefits: benefitsSchema,

    isActive: {
      type: Boolean,
      default: true,
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

// Composite uniqueness
subscriptionPlanSchema.index({ name: 1, version: 1 }, { unique: true });

export default mongoose.model("SubscriptionPlan", subscriptionPlanSchema);
