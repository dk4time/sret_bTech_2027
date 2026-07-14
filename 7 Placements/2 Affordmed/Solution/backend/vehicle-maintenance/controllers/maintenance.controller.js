import { successResponse } from "../../../common/apiResponse/apiResponse.js";
import { scheduleMaintenanceService } from "../services/maintenance.service.js";

export async function scheduleMaintenance(req, res, next) {
  try {
    const result = await scheduleMaintenanceService(req.body);

    return successResponse(
      res,
      result,
      "Maintenance schedule generated successfully.",
    );
  } catch (error) {
    next(error);
  }
}
