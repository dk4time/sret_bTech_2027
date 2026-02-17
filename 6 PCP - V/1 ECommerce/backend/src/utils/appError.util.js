class AppError extends Error {
  constructor(
    message,
    statusCode = 500,
    errorCode = "SERVER_ERROR",
    details = null,
  ) {
    super(message);

    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.details = details;
    this.isOperational = true;

    Error.captureStackTrace(this, this.constructor);
  }
}

export default AppError;
