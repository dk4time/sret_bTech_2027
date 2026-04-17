import express from "express";
import {
  getTodos,
  addTodo,
  deleteTodo,
  updateTodo,
} from "../controllers/todosController.js";

const router = express.Router();

// GET ?_sort=order
router.get("/", getTodos);

// POST
router.post("/", addTodo);

// DELETE
router.delete("/:id", deleteTodo);

// PATCH
router.patch("/:id", updateTodo);

export default router;
