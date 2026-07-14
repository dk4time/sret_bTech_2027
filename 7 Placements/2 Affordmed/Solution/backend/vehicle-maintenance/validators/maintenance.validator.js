import { AppError } from "../../../common/errors/AppError.js";

export function validateMaintenanceRequest(body) {
  if (!body) {
    throw new AppError("Request body is required.", 400);
  }

  let {
    availableHours,

    vehicles,
  } = body;

  availableHours = Number(availableHours);

  if (Number.isNaN(availableHours)) {
    throw new AppError("Available hours should be numeric.", 400);
  }

  if (availableHours <= 0) {
    throw new AppError("Available hours should be greater than zero.", 400);
  }

  if (!Array.isArray(vehicles)) {
    throw new AppError("Vehicles should be an array.", 400);
  }

  if (vehicles.length === 0) {
    throw new AppError("Vehicle list cannot be empty.", 400);
  }

  const sanitizedVehicles = vehicles.map((vehicle) => {
    const {
      id,

      name,

      maintenanceHours,

      priority,
    } = vehicle;

    if (!id) throw new AppError("Vehicle id is required.", 400);

    if (!name?.trim()) throw new AppError("Vehicle name is required.", 400);

    const hours = Number(maintenanceHours);

    const score = Number(priority);

    if (Number.isNaN(hours) || hours <= 0) {
      throw new AppError("Invalid maintenance hours.", 400);
    }

    if (Number.isNaN(score) || score <= 0) {
      throw new AppError("Invalid priority.", 400);
    }

    return {
      id,

      name: name.trim(),

      maintenanceHours: hours,

      priority: score,
    };
  });

  return {
    availableHours,

    vehicles: sanitizedVehicles,
  };
}
