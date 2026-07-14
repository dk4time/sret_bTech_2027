export class AppError extends Error {
  constructor(message, statusCode = 500) {
    super(message);

    this.statusCode = statusCode;
    this.status = "error";

    Error.captureStackTrace(this, this.constructor);
  }
}
