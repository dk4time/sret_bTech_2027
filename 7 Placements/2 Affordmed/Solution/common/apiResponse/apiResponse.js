export function successResponse(
  res,
  data = null,
  message = "Success",
  statusCode = 200,
) {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
  });
}

export function errorResponse(
  res,
  message = "Something went wrong",
  statusCode = 500,
  errors = null,
) {
  return res.status(statusCode).json({
    success: false,
    message,
    errors,
  });
}
