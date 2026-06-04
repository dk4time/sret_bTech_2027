import { useEvents } from "../context/EventContext";
import { isValidEvent } from "../utils/validateEvent";
import EventCard from "../components/EventCard";

const Events = () => {
  const { events, loading } = useEvents();

  // ✅ Safety
  const safeEvents = Array.isArray(events) ? events : [];

  // 🔥 CORE (IMPORTANT)
  const validEvents = safeEvents.filter(isValidEvent);

  if (loading) return <p className="loading">Loading events...</p>;

  if (!validEvents.length) {
    return <h3>No valid events available</h3>;
  }

  return (
    <div className="grid">
      {validEvents.map((event) => (
        <EventCard key={event.eventId} event={event} />
      ))}
    </div>
  );
};

export default Events;
