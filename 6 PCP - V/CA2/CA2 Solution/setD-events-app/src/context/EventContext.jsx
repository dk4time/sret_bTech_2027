import { createContext, useContext, useReducer, useEffect } from "react";
import axios from "axios";
import EventReducer from "../reducer/EventReducer";
import { getToken, getDataset } from "../api/api.jsx";

// INITIAL STATE
const initialState = {
  events: [],
  loading: true,
};

export const EventContext = createContext();

export const EventProvider = ({ children }) => {
  const [state, dispatch] = useReducer(EventReducer, initialState);

  // 🔥 FETCH DATA
  useEffect(() => {
    const fetchData = async () => {
      const tokenRes = await getToken("20084016", "264006", "setD");
      const data = await getDataset(tokenRes.token, tokenRes.dataUrl);
      console.log("Fetched events:", data);
      dispatch({ type: "SET_EVENTS", payload: data.events });
    };

    fetchData();
  }, []);

  const updateBooking = (id, ticketsSold) =>
    dispatch({
      type: "UPDATE_BOOKING",
      payload: { id, ticketsSold },
    });

  return (
    <EventContext.Provider
      value={{
        events: state.events,
        loading: state.loading,
        updateBooking,
      }}
    >
      {children}
    </EventContext.Provider>
  );
};

export const useEvents = () => useContext(EventContext);
