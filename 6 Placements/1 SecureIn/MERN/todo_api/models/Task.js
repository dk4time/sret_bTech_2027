import mongoose from "mongoose";

const taskSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: true,
      trim: true,
      lowercase: true,
      index: true,
    },

    description: {
      type: String,
      trim: true,
    },

    status: {
      type: String,
      enum: ["todo", "in-progress", "done"],
      default: "todo",
      index: true,
    },

    dueDate: {
      type: Date,
      index: true,
    },

    assignedTo: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      index: true,
    },
  },
  { timestamps: true },
);

// Compound Unique Index (Prevents duplicates per user)
taskSchema.index({ title: 1, assignedTo: 1 }, { unique: true });

// Query Optimization Index
taskSchema.index({ status: 1, dueDate: 1 });

const Task = mongoose.model("Task", taskSchema);

export default Task;
