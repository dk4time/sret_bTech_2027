import axiosInstance from "../config/axios.js";
import { validateLogger } from "../validators/logger.validator.js";

const BASE_URL = process.env.LOGGER_BASE_URL;
const TOKEN = process.env.LOGGER_TOKEN;

export async function Log(log) {
  try {
    validateLogger(log);

    const { stack, level, packageName, message } = log;

    await axiosInstance.post(
      BASE_URL,
      {
        stack,
        level,
        package: packageName,
        message,
      },
      {
        headers: {
          Authorization: `Bearer ${TOKEN}`,
        },
      },
    );
  } catch {
    // Fire and Forget
  }
}
