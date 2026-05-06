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

    project: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Project",
      index: true,
    },
  },
  { timestamps: true },
);

taskSchema.index({ status: 1, dueDate: 1 });

taskSchema.index({ title: 1, project: 1 }, { unique: true });

taskSchema.index({
  title: "text",
  description: "text",
});

const Task = mongoose.model("Task", taskSchema);

export default Task;
