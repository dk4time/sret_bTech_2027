import { useActivities } from "../context/ActivityContext";
import { isValidActivity } from "../utils/validateActivity";
import ActivityCard from "../components/ActivityCard";

const Activities = () => {
  const { activities, loading } = useActivities();

  if (loading) return <p>Loading...</p>;

  // ✅ Q1 CORE LOGIC
  const validActivities = activities.filter(isValidActivity);

  return (
    <div className="container">
      <h2>Activities</h2>

      {validActivities.length === 0 ? (
        <p>No valid activities</p>
      ) : (
        validActivities.map((activity) => (
          <ActivityCard key={activity.activityId} activity={activity} />
        ))
      )}
    </div>
  );
};

export default Activities;
