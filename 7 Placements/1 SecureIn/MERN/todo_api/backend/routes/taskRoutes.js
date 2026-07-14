import express from "express";
import mongoose from "mongoose";
import Task from "../models/Task.js";
import Project from "../models/Project.js";

const router = express.Router();

/*
====================================================
1. CREATE TASK
====================================================
*/
router.post("/", async (req, res) => {
  try {
    const { title, description, status, dueDate, assignedTo, project } =
      req.body;

    if (!title) {
      return res.status(400).json({
        message: "Title is required",
      });
    }

    const task = await Task.create({
      title,
      description,
      status,
      dueDate,
      assignedTo,
      project,
    });

    await task.populate("assignedTo", "name email");
    await task.populate("project", "name");

    res.status(201).json(task);
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({
        message: "Duplicate task not allowed",
      });
    }

    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
2. GET TASKS
Pagination + Filtering + Search
====================================================
*/
router.get("/", async (req, res) => {
  try {
    let {
      page = 1,
      limit = 10,
      status,
      userId,
      projectId,
      overdue,
      search,
    } = req.query;

    page = parseInt(page);
    limit = parseInt(limit);

    const filter = {};

    /*
    ============================================
    FILTERS
    ============================================
    */

    if (status) {
      filter.status = status;
    }

    if (userId) {
      filter.assignedTo = userId;
    }

    if (projectId) {
      filter.project = projectId;
    }

    /*
    ============================================
    OVERDUE
    ============================================
    */

    if (overdue === "true") {
      filter.dueDate = {
        $lt: new Date(),
      };

      filter.status = {
        $ne: "done",
      };
    }

    /*
    ============================================
    SEARCH
    ============================================
    */

    if (search) {
      filter.title = {
        $regex: search,
        $options: "i",
      };
    }

    /*
    ============================================
    TOTAL COUNT
    ============================================
    */

    const total = await Task.countDocuments(filter);

    /*
    ============================================
    FETCH TASKS
    ============================================
    */

    const tasks = await Task.find(filter)
      .populate("assignedTo", "name email")
      .populate("project", "name")
      .sort({
        createdAt: -1,
      })
      .skip((page - 1) * limit)
      .limit(limit);

    res.json({
      total,
      page,
      limit,
      totalPages: Math.ceil(total / limit),

      data: tasks,
    });
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
3. SPECIAL ROUTES
(MUST COME BEFORE /:id)
====================================================
*/

/*
====================================================
STATS
====================================================
*/
router.get("/stats", async (req, res) => {
  try {
    const stats = await Task.aggregate([
      {
        $group: {
          _id: "$status",
          count: {
            $sum: 1,
          },
        },
      },
    ]);

    res.json(stats);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
STATS PER USER
====================================================
*/
router.get("/stats/user/:userId", async (req, res) => {
  try {
    const userId = new mongoose.Types.ObjectId(req.params.userId);

    const stats = await Task.aggregate([
      {
        $match: {
          assignedTo: userId,
        },
      },

      {
        $group: {
          _id: "$status",

          count: {
            $sum: 1,
          },
        },
      },
    ]);

    res.json(stats);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
RECENT TASKS
====================================================
*/
router.get("/recent", async (req, res) => {
  try {
    const tasks = await Task.find()
      .populate("assignedTo", "name email")
      .populate("project", "name")
      .sort({
        createdAt: -1,
      })
      .limit(5);

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
TASKS DUE SOON
====================================================
*/
router.get("/due-soon", async (req, res) => {
  try {
    const today = new Date();

    const next7Days = new Date();

    next7Days.setDate(today.getDate() + 7);

    const tasks = await Task.find({
      dueDate: {
        $gte: today,
        $lte: next7Days,
      },
    })
      .populate("assignedTo", "name email")
      .populate("project", "name");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
TASKS DUE ON SPECIFIC DATE
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
    })
      .populate("assignedTo", "name email")
      .populate("project", "name");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
OVERDUE TASKS
====================================================
*/
router.get("/overdue/list", async (req, res) => {
  try {
    const tasks = await Task.find({
      dueDate: {
        $lt: new Date(),
      },

      status: {
        $ne: "done",
      },
    })
      .populate("assignedTo", "name email")
      .populate("project", "name");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
TASKS OF SPECIFIC USER
====================================================
*/
router.get("/user/:userId", async (req, res) => {
  try {
    const tasks = await Task.find({
      assignedTo: req.params.userId,
    })
      .populate("assignedTo", "name email")
      .populate("project", "name");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
UNASSIGNED TASKS
====================================================
*/
router.get("/unassigned/list", async (req, res) => {
  try {
    const tasks = await Task.find({
      $or: [
        {
          assignedTo: {
            $exists: false,
          },
        },

        {
          assignedTo: null,
        },
      ],
    }).populate("project", "name");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
ADVANCED FILTER
====================================================
*/
router.get("/filter", async (req, res) => {
  try {
    const { userId } = req.query;

    const tasks = await Task.find({
      assignedTo: userId,

      status: {
        $ne: "done",
      },

      dueDate: {
        $lt: new Date(),
      },
    })
      .populate("assignedTo", "name email")
      .populate("project", "name");

    res.json(tasks);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
4. GET TASK BY ID
(MUST BE LAST GET ROUTE)
====================================================
*/
router.get("/:id", async (req, res) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({
        message: "Invalid ID",
      });
    }

    const task = await Task.findById(req.params.id)
      .populate("assignedTo", "name email")
      .populate("project", "name");

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    res.json(task);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
5. PATCH
Partial Update
====================================================
*/
router.patch("/:id", async (req, res) => {
  try {
    const task = await Task.findByIdAndUpdate(
      req.params.id,

      {
        $set: req.body,
      },

      {
        returnDocument: "after",

        runValidators: true,

        upsert: true,
      },
    )
      .populate("assignedTo", "name email")
      .populate("project", "name");

    res.json(task);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
ASSIGN TASK TO USER
====================================================
*/
router.patch("/:id/assign", async (req, res) => {
  try {
    const { userId } = req.body;

    /*
    ============================================
    VALIDATE USER ID
    ============================================
    */

    if (!mongoose.isValidObjectId(userId)) {
      return res.status(400).json({
        message: "Invalid userId",
      });
    }

    /*
    ============================================
    GET TASK
    ============================================
    */

    const task = await Task.findById(req.params.id);

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    /*
    ============================================
    GET PROJECT
    ============================================
    */

    const project = await Project.findById(task.project);

    if (!project) {
      return res.status(404).json({
        message: "Project not found",
      });
    }

    /*
    ============================================
    VALIDATE MEMBERSHIP
    ============================================
    */

    const isOwner = project.owner.toString() === userId;

    const isMember = project.members.some(
      (member) => member.user.toString() === userId,
    );

    if (!isOwner && !isMember) {
      return res.status(400).json({
        message: "User not part of this project",
      });
    }

    /*
    ============================================
    ASSIGN TASK
    ============================================
    */

    task.assignedTo = userId;

    await task.save();

    await task.populate("assignedTo", "name email");

    await task.populate("project", "name");

    res.json(task);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
UNASSIGN TASK
====================================================
*/
router.patch("/:id/unassign", async (req, res) => {
  try {
    const task = await Task.findByIdAndUpdate(
      req.params.id,

      {
        assignedTo: null,
      },

      {
        returnDocument: "after",
      },
    ).populate("project", "name");

    res.json(task);
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
6. PUT
Full Replace
====================================================
*/
router.put("/:id", async (req, res) => {
  try {
    const task = await Task.findByIdAndReplace(
      req.params.id,

      req.body,

      {
        returnDocument: "after",

        runValidators: true,
      },
    )
      .populate("assignedTo", "name email")
      .populate("project", "name");

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    res.json(task);
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({
        message: "Duplicate task in this project",
      });
    }

    res.status(500).json({
      message: err.message,
    });
  }
});

/*
====================================================
7. DELETE TASK
====================================================
*/
router.delete("/:id", async (req, res) => {
  try {
    const task = await Task.findByIdAndDelete(req.params.id);

    if (!task) {
      return res.status(404).json({
        message: "Task not found",
      });
    }

    res.json({
      message: "Task deleted",
    });
  } catch (err) {
    res.status(500).json({
      message: err.message,
    });
  }
});

export default router;
