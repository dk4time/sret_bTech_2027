import { createContext, useContext, useReducer, useEffect } from "react";
import CourseReducer from "../reducer/CourseReducer";
import axios from "axios";
import { getToken, getDataset } from "../api/api.jsx";

// INITIAL STATE
const initialState = {
  courses: [],
  loading: true,
};

export const CourseContext = createContext();

export const CourseProvider = ({ children }) => {
  const [state, dispatch] = useReducer(CourseReducer, initialState);

  // 🔥 FETCH DATA
  useEffect(() => {
    const fetchData = async () => {
      const tokenRes = await getToken("20084016", "264006", "setC");
      const data = await getDataset(tokenRes.token, tokenRes.dataUrl);
      console.log("Fetched courses:", data);
      dispatch({ type: "SET_COURSES", payload: data.courses });
    };

    fetchData();
  }, []);

  // ACTIONS
  const toggleActive = (id) => dispatch({ type: "TOGGLE_ACTIVE", payload: id });

  return (
    <CourseContext.Provider
      value={{
        courses: state.courses,
        loading: state.loading,
        toggleActive,
      }}
    >
      {children}
    </CourseContext.Provider>
  );
};

export const useCourses = () => useContext(CourseContext);
