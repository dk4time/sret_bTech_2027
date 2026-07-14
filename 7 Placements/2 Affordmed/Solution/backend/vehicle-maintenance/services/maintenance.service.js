import { Log } from "../../../common/logger/index.js";

import { validateMaintenanceRequest } from "../validators/maintenance.validator.js";

import { scheduleVehicles } from "../utils/schedule.js";

export async function scheduleMaintenanceService(body) {
  await Log({
    stack: "backend",
    level: "info",
    package: "service",
    message: "Vehicle maintenance scheduling started.",
  });

  const validatedData = validateMaintenanceRequest(body);

  const schedule = scheduleVehicles(validatedData);

  await Log({
    stack: "backend",
    level: "info",
    package: "service",
    message: "Vehicle maintenance scheduling completed.",
  });

  return schedule;
}
