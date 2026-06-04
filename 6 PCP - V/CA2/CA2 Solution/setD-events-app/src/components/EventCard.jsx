import { useNavigate } from "react-router-dom";
import { useEvents } from "../context/EventContext";

const EventCard = ({ event }) => {
  const navigate = useNavigate();
  const { correctStatus } = useEvents();

  return (
    <div
      className="card"
      data-testid="event-item"
      onClick={() => navigate(`/events/${event.eventId}`)}
    >
      <h3>{event.eventName || "Unnamed Event"}</h3>

      <p>Location: {event.location || "Unknown"}</p>

      <p>Price: ₹{event.ticketPrice}</p>

      <p>
        Tickets: {event.ticketsSold}/{event.ticketsAvailable}
      </p>

      <p>Status: {event.status}</p>
    </div>
  );
};

export default EventCard;
