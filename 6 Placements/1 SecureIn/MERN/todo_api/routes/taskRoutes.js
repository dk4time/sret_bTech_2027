import express from "express";
import mongoose from "mongoose";
import Task from "../models/Task.js";

const router = express.Router();

/*
====================================================
1. CREATE TASK
----------------------------------------------------
Creates a new task with controlled fields.
Prevents duplicate via DB index (handled in catch).
====================================================
*/
router.post("/", async (req, res) => {
  try {
    const { title, description, status, dueDate, assignedTo, project } =
      req.body;

    if (!title) {
      return res.status(400).json({ message: "Title is required" });
    }

    const task = await Task.create({
      title,
      description,
      status,
      dueDate,
      assignedTo,
      project,
    });

    res.status(201).json(task);
  } catch (err) {
    // Duplicate key error (unique index violation)
    if (err.code === 11000) {
      return res.status(409).json({
        message: "Duplicate task not allowed",
      });
    }

    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
2. GET ALL TASKS (WITH FILTER + POPULATE)
----------------------------------------------------
Supports query params:
?status=todo
?userId=xxx
?overdue=true
====================================================
*/
router.get("/", async (req, res) => {
  try {
    const { status, userId, overdue } = req.query;

    let filter = {};

    // Filter by status
    if (status) {
      filter.status = status;
    }

    // Filter by assigned user
    if (userId) {
      filter.assignedTo = userId;
    }

    // Overdue tasks (due date passed & not completed)
    if (overdue === "true") {
      filter.dueDate = { $lt: new Date() };
      filter.status = { $ne: "done" };
    }

    const tasks = await Task.find(filter)
      .populate("assignedTo", "name email")
      .populate("project", "name")
      .sort({ createdAt: -1 });

    res.json(tasks);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
3. GET TASK BY ID
----------------------------------------------------
Returns a single task using MongoDB _id
====================================================
*/
router.get("/:id", async (req, res) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ message: "Invalid ID" });
    }

    const task = await Task.findById(req.params.id)
      .populate("assignedTo", "name email")
      .populate("project", "name");

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    res.json(task);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
4. UPDATE TASK (PATCH - PARTIAL UPDATE)
----------------------------------------------------
Updates only provided fields.
Validators are enabled.
====================================================
*/
router.patch("/:id", async (req, res) => {
  try {
    const task = await Task.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    res.json(task);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
5. DELETE TASK
----------------------------------------------------
Deletes a task by ID
====================================================
*/
router.delete("/:id", async (req, res) => {
  try {
    const task = await Task.findByIdAndDelete(req.params.id);

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    res.json({ message: "Task deleted" });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
6. TASKS DUE ON A SPECIFIC DATE
----------------------------------------------------
Example:
GET /api/tasks/due/2026-06-01
====================================================
*/
router.get("/due/:date", async (req, res) => {
  try {
    const date = new Date(req.params.date);

    const nextDay = new Date(date);
    nextDay.setDate(nextDay.getDate() + 1);

    const tasks = await Task.find({
      dueDate: {
        $gte: date,
        $lt: nextDay,
      },
    });

    res.json(tasks);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
7. OVERDUE TASKS
----------------------------------------------------
Tasks whose due date is passed and not completed
====================================================
*/
router.get("/overdue/list", async (req, res) => {
  try {
    const tasks = await Task.find({
      dueDate: { $lt: new Date() },
      status: { $ne: "done" },
    });

    res.json(tasks);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
8. TASKS ASSIGNED TO A USER
----------------------------------------------------
Example:
GET /api/tasks/user/:userId
====================================================
*/
router.get("/user/:userId", async (req, res) => {
  try {
    const tasks = await Task.find({
      assignedTo: req.params.userId,
    }).populate("assignedTo", "name email");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
9. UNASSIGNED TASKS
----------------------------------------------------
Tasks that have no assigned user
====================================================
*/
router.get("/unassigned/list", async (req, res) => {
  try {
    const tasks = await Task.find({
      $or: [{ assignedTo: { $exists: false } }, { assignedTo: null }],
    });

    res.json(tasks);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/*
====================================================
10. ADVANCED FILTER
----------------------------------------------------
Example:
GET /api/tasks/filter?userId=xxx
Returns:
- Assigned to user
- Not completed
- Overdue
====================================================
*/
router.get("/filter", async (req, res) => {
  try {
    const { userId } = req.query;

    const tasks = await Task.find({
      assignedTo: userId,
      status: { $ne: "done" },
      dueDate: { $lt: new Date() },
    });

    res.json(tasks);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

export default router;
