import { v4 as uuidv4 } from "uuid";

let todos = [];

// GET /todos?_sort=order
export const getTodos = (req, res) => {
  let result = [...todos];

  if (req.query._sort === "order") {
    result.sort((a, b) => a.order - b.order);
  }

  res.json(result);
};

// POST /todos
export const addTodo = (req, res) => {
  const { text, completed, order } = req.body;

  const newTodo = {
    id: uuidv4(),
    text,
    completed,
    order,
  };

  todos.push(newTodo);
  res.status(201).json(newTodo);
};

// DELETE /todos/:id
export const deleteTodo = (req, res) => {
  const { id } = req.params;

  todos = todos.filter((todo) => todo.id !== id);

  res.json({ message: "Todo deleted" });
};

// PATCH /todos/:id
export const updateTodo = (req, res) => {
  const { id } = req.params;
  const updates = req.body;

  todos = todos.map((todo) =>
    todo.id === id ? { ...todo, ...updates } : todo,
  );

  const updatedTodo = todos.find((todo) => todo.id === id);

  res.json(updatedTodo);
};
