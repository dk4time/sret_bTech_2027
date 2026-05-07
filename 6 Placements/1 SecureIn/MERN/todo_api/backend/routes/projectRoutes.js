import express from "express";
import mongoose from "mongoose";
import Project from "../models/Project.js";

const router = express.Router();

/*
====================================================
1. CREATE PROJECT
----------------------------------------------------
Creates a project with owner and optional members
====================================================
*/
router.post("/", async (req, res) => {
  try {
    const { name, description, owner, members } = req.body;

    if (!name || !owner) {
      return res.status(400).json({
        message: "name and owner are required",
      });
    }

    if (!mongoose.isValidObjectId(owner)) {
      return res.status(400).json({ message: "Invalid owner id" });
    }

    const project = await Project.create({
      name,
      description,
      owner,
      members,
    });

    res.status(201).json(project);
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({
        message: "Duplicate project name for this owner",
      });
    }

    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
2. GET ALL PROJECTS
----------------------------------------------------
Populates owner and members
====================================================
*/
router.get("/", async (req, res) => {
  try {
    const projects = await Project.find()
      .populate("owner", "name email")
      .populate("members.user", "name email")
      .sort({ createdAt: -1 });

    res.json(projects);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
3. GET PROJECT BY ID
====================================================
*/
router.get("/:id", async (req, res) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ message: "Invalid ID" });
    }

    const project = await Project.findById(req.params.id)
      .populate("owner", "name email")
      .populate("members.users", "name email");

    if (!project) {
      return res.status(404).json({ message: "Project not found" });
    }

    res.json(project);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
4. PATCH PROJECT (PARTIAL UPDATE)
====================================================
*/
router.patch("/:id", async (req, res) => {
  try {
    const project = await Project.findByIdAndUpdate(
      req.params.id,
      { $set: req.body },
      {
        returnDocument: "after",
        runValidators: true,
      },
    );

    if (!project) {
      return res.status(404).json({ message: "Project not found" });
    }

    res.json(project);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
5. DELETE PROJECT
====================================================
*/
router.delete("/:id", async (req, res) => {
  try {
    const project = await Project.findByIdAndDelete(req.params.id);

    if (!project) {
      return res.status(404).json({ message: "Project not found" });
    }

    res.json({ message: "Project deleted" });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

export default router;
