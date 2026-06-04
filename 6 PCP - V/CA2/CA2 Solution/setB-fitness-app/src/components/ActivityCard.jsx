import { useNavigate } from "react-router-dom";
import { useActivities } from "../context/ActivityContext";

const ActivityCard = ({ activity }) => {
  const navigate = useNavigate();
  const { toggleGoal } = useActivities();

  return (
    <div
      className="card"
      data-testid="activity-item" // ✅ MANDATORY
      onClick={() => navigate(`/activities/${activity.activityId}`)}
      style={{ cursor: "pointer" }}
    >
      {/* ✅ EDGE CASES */}
      <h3>{activity.name || "Unknown"}</h3>

      <p>Steps: {activity.steps}</p>
      <p>Calories: {activity.caloriesBurned}</p>
      <p>Minutes: {activity.workoutMinutes}</p>

      <p>Date: {activity.date || "No Date"}</p>

      {/* ✅ DISPLAY STATUS */}
      <p>
        Goal:{" "}
        <strong>{activity.goalAchieved ? "Achieved" : "Not Achieved"}</strong>
      </p>

      {/* ✅ KEEP BUTTON FOR Q4 */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          toggleGoal(activity.activityId);
        }}
      >
        {activity.steps >= 8000 ? "Auto Achieved" : "Toggle Goal"}
      </button>
    </div>
  );
};

export default ActivityCard;
