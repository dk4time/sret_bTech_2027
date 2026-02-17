export const successResponse = (
  res,
  data = null,
  message = "Success",
  statusCode = 200,
) => {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
  });
};

export const errorResponse = (
  res,
  message = "Something went wrong",
  statusCode = 500,
  errorCode = "SERVER_ERROR",
  details = null,
) => {
  return res.status(statusCode).json({
    success: false,
    message,
    errorCode,
    details,
  });
};
