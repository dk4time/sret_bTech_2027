import mongoose from "mongoose";

const projectSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
      lowercase: true,
    },

    description: {
      type: String,
      trim: true,
    },

    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },

    members: [
      {
        user: {
          type: mongoose.Schema.Types.ObjectId,
          ref: "User",
        },
        role: {
          type: String,
          enum: ["member", "admin"],
          default: "member",
        },
      },
    ],
  },
  { timestamps: true },
);

// prevent duplicate project name per owner
projectSchema.index({ name: 1, owner: 1 }, { unique: true });

const Project = mongoose.model("Project", projectSchema);

export default Project;
