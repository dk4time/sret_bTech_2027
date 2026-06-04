export const isValidActivity = (activity) => {
  if (!activity) return false;

  if (typeof activity.steps !== "number" || activity.steps <= 0) {
    return false;
  }

  if (
    typeof activity.caloriesBurned !== "number" ||
    activity.caloriesBurned <= 0
  ) {
    return false;
  }

  if (
    typeof activity.workoutMinutes !== "number" ||
    activity.workoutMinutes <= 0
  ) {
    return false;
  }

  if (typeof activity.goalAchieved !== "boolean") {
    return false;
  }

  return true;
};
