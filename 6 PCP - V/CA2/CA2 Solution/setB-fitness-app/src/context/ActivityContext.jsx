import { createContext, useReducer, useEffect, useContext } from "react";
import axios from "axios";
import ActivityReducer from "../reducer/ActivityReducer";
import { getToken, getDataset } from "../api/api.jsx";

const initialState = {
  activities: [],
  loading: true,
};

export const ActivityContext = createContext();

export const ActivityProvider = ({ children }) => {
  const [state, dispatch] = useReducer(ActivityReducer, initialState);

  useEffect(() => {
    const fetchData = async () => {
      const tokenRes = await getToken("20084016", "264006", "setB");
      const data = await getDataset(tokenRes.token, tokenRes.dataUrl);
      console.log("Fetched activities:", data);
      dispatch({ type: "SET_ACTIVITIES", payload: data.activities });
    };

    fetchData();
  }, []);

  const toggleGoal = (id) => dispatch({ type: "TOGGLE_GOAL", payload: id });

  return (
    <ActivityContext.Provider
      value={{
        activities: state.activities,
        loading: state.loading,
        toggleGoal,
      }}
    >
      {children}
    </ActivityContext.Provider>
  );
};

export const useActivities = () => useContext(ActivityContext);
