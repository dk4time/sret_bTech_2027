import AppError from "../utils/appError.util.js";
import { errorResponse } from "../utils/response.util.js";

const errorMiddleware = (err, req, res, next) => {
  console.error("❌ ERROR:", err);

  // 1️⃣ Mongoose Validation Error
  if (err.name === "ValidationError") {
    return errorResponse(
      res,
      "Validation failed",
      400,
      "VALIDATION_ERROR",
      err.errors,
    );
  }

  // 2️⃣ Duplicate Key Error
  if (err.code === 11000) {
    return errorResponse(res, "Duplicate field value", 400, "DUPLICATE_ERROR");
  }

  // 3️⃣ Invalid ObjectId
  if (err.name === "CastError") {
    return errorResponse(res, "Invalid resource ID", 400, "INVALID_ID");
  }

  // 4️⃣ Custom AppError
  if (err instanceof AppError) {
    return errorResponse(
      res,
      err.message,
      err.statusCode,
      err.errorCode,
      err.details,
    );
  }

  // 5️⃣ Unknown Errors
  return errorResponse(res, "Internal Server Error", 500, "SERVER_ERROR");
};

export default errorMiddleware;
