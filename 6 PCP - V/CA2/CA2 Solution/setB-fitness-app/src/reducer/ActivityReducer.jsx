import { isValidActivity } from "../utils/validateActivity";

const ActivityReducer = (state, action) => {
  switch (action.type) {
    case "SET_ACTIVITIES":
      return {
        ...state,
        activities: action.payload,
        loading: false,
      };

    case "TOGGLE_GOAL":
      return {
        ...state,
        activities: state.activities.map((a) => {
          // ❗ Match activity
          if (a.activityId !== action.payload) return a;

          // ❗ Ignore invalid activity
          if (!isValidActivity(a)) return a;

          // 🔥 BUSINESS RULE
          if (a.steps >= 8000) {
            // Already correct → no duplicate update
            if (a.goalAchieved === true) return a;

            return { ...a, goalAchieved: true };
          }

          // ✅ Normal toggle
          return { ...a, goalAchieved: !a.goalAchieved };
        }),
      };

    default:
      return state;
  }
};

export default ActivityReducer;
