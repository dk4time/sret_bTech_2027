import mongoose from "mongoose";

const taskSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: true,
      index: true,
    },

    description: String,

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
  },
  { timestamps: true },
);

const Task = mongoose.model("Task", taskSchema);

export default Task;
