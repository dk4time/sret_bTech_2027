import { useParams } from "react-router-dom";
import { useActivities } from "../context/ActivityContext";

const ActivityDetail = () => {
  const { id } = useParams();
  const { activities } = useActivities();

  // ✅ Safe array
  const safeActivities = Array.isArray(activities) ? activities : [];

  // ✅ Find activity (IMPORTANT)
  const activity = safeActivities.find((a) => a.activityId === Number(id));

  // ❌ Invalid ID
  if (!activity) {
    return <h3>Activity not found</h3>;
  }

  // ✅ Safe efficiency calculation
  const minutes =
    typeof activity.workoutMinutes === "number" ? activity.workoutMinutes : 0;

  const calories =
    typeof activity.caloriesBurned === "number" ? activity.caloriesBurned : 0;

  const efficiency = minutes > 0 ? (calories / minutes).toFixed(2) : "N/A";

  return (
    <div className="container">
      <h2>Activity Detail</h2>

      <p>
        <strong>Name:</strong> {activity.name || "Unknown"}
      </p>

      <p>
        <strong>Steps:</strong> {activity.steps}
      </p>

      <p>
        <strong>Calories:</strong> {activity.caloriesBurned}
      </p>

      <p>
        <strong>Workout Minutes:</strong> {activity.workoutMinutes}
      </p>

      <p>
        <strong>Date:</strong> {activity.date || "No Date"}
      </p>

      <p>
        <strong>Goal:</strong>{" "}
        {activity.goalAchieved ? "Achieved" : "Not Achieved"}
      </p>

      {/* ✅ Computed Value */}
      <p>
        <strong>Efficiency:</strong> {efficiency}
      </p>
    </div>
  );
};

export default ActivityDetail;
