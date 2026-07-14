import { LOG_LEVELS, STACKS, PACKAGES } from "../constants/logger.constants.js";

export function validateLogger(log) {
  const { stack, level, packageName, message } = log;

  if (!STACKS.includes(stack)) throw new Error("Invalid stack.");

  if (!LOG_LEVELS.includes(level)) throw new Error("Invalid log level.");

  if (!PACKAGES.includes(packageName)) throw new Error("Invalid package.");

  if (typeof message !== "string" || message.trim() === "") {
    throw new Error("Message is required.");
  }
}
