const EventReducer = (state, action) => {
  switch (action.type) {
    case "SET_EVENTS":
      return {
        ...state,
        events: action.payload,
        loading: false,
      };

    case "CORRECT_STATUS":
      return {
        ...state,
        events: state.events.map((e) => {
          if (!e || e.eventId !== action.payload) return e;

          // ❗ RULE 1: Cancelled → never change
          if (e.status === "Cancelled") return e;

          const available =
            typeof e.ticketsAvailable === "number" ? e.ticketsAvailable : 0;

          const sold = typeof e.ticketsSold === "number" ? e.ticketsSold : 0;

          // ❗ RULE 2: Sold Out condition
          if (sold === available && available > 0) {
            return { ...e, status: "Sold Out" };
          }

          // ❗ OTHERWISE → no change
          return e;
        }),
      };

    case "UPDATE_BOOKING":
      return {
        ...state,
        events: state.events.map((e) => {
          if (e.eventId !== action.payload.id) return e;

          const newSold = action.payload.ticketsSold;

          const available =
            typeof e.ticketsAvailable === "number" ? e.ticketsAvailable : 0;

          const currentSold =
            typeof e.ticketsSold === "number" ? e.ticketsSold : 0;

          // ❗ VALIDATION
          if (
            typeof newSold !== "number" ||
            newSold <= currentSold ||
            newSold > available
          ) {
            return e; // ignore invalid update
          }

          let updatedEvent = {
            ...e,
            ticketsSold: newSold,
          };

          // 🔥 APPLY Q4 BUSINESS RULE AUTOMATICALLY
          if (updatedEvent.status !== "Cancelled") {
            if (newSold === available && available > 0) {
              updatedEvent.status = "Sold Out";
            }
          }

          return updatedEvent;
        }),
      };

    default:
      return state;
  }
};

export default EventReducer;
