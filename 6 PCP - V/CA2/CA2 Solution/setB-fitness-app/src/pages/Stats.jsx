import { useEffect } from "react";
import { useActivities } from "../context/ActivityContext";
import { isValidActivity } from "../utils/validateActivity";

const Stats = () => {
  const { activities } = useActivities();

  // ✅ Q1 VALIDATION REUSE
  const validActivities = activities.filter(isValidActivity);

  // ✅ Q5 CORE LOGIC (MUST USE REDUCE)
  const stats = validActivities.reduce(
    (acc, a) => {
      // ❗ Ignore invalid goalAchieved values
      if (typeof a.goalAchieved !== "boolean") return acc;

      acc.totalActivities++;

      if (a.goalAchieved === true) {
        acc.goalAchievedCount++;
      } else {
        acc.goalNotAchievedCount++;
      }

      return acc;
    },
    {
      totalActivities: 0,
      goalAchievedCount: 0,
      goalNotAchievedCount: 0,
    },
  );

  // ✅ GLOBAL STATE (MANDATORY)
  useEffect(() => {
    window.appState = stats;
  }, [activities]);

  return (
    <div className="container">
      <h2>Activity Analytics</h2>

      {/* ✅ TEST IDs (VERY IMPORTANT) */}
      <div data-testid="total-activities">
        Total Activities: {stats.totalActivities}
      </div>

      <div data-testid="goal-achieved">
        Goal Achieved: {stats.goalAchievedCount}
      </div>

      <div data-testid="goal-not-achieved">
        Goal Not Achieved: {stats.goalNotAchievedCount}
      </div>
    </div>
  );
};

export default Stats;
