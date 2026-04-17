import express from "express";
import cors from "cors";
import todoRoutes from "./routes/todos.js";

const app = express();
const PORT = 5050;

app.use(cors());
app.use(express.json());

// Routes
app.use("/todos", todoRoutes);

app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
