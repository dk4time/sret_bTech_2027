import express from "express";
import cookieParser from "cookie-parser";
import routes from "./src/routes.js";
import connectDB from "./src/config/db.js";
import { ENV } from "./src/config/env.js";
import errorMiddleware from "./src/middleware/error.middleware.js";

const app = express();

app.use(express.json());
app.use(cookieParser());

app.use("/api", routes);

app.use("/", () => {
  console.log("API is running...");
});

app.use(errorMiddleware);

const startServer = async () => {
  await connectDB();

  app.listen(ENV.PORT, () => {
    console.log(`Server running on port ${ENV.PORT}`);
  });
};

startServer();
