import { useState } from "react";
import { useEvents } from "../context/EventContext";
import { isValidEvent } from "../utils/validateEvent";
import EventCard from "../components/EventCard";

const Filter = () => {
  const { events } = useEvents();

  const [location, setLocation] = useState("");
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);

  // ✅ Q1 dependency
  const safeEvents = Array.isArray(events) ? events : [];
  const validEvents = safeEvents.filter(isValidEvent);

  // 🔥 CORE FILTER (case-insensitive)
  const filteredEvents = validEvents.filter((e) =>
    e.location?.toLowerCase().includes(location.toLowerCase()),
  );

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!location.trim()) {
      setError("Please enter location");
      setSubmitted(false);
      return;
    }

    setError("");
    setSubmitted(true);
  };

  return (
    <div className="container">
      <h2>Filter Events</h2>

      {/* FORM */}
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Enter location"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          data-testid="filter-input"
        />

        <button type="submit">Search</button>
      </form>

      {/* ERROR */}
      {error && <p className="error">{error}</p>}

      {/* NO RESULT */}
      {submitted && !filteredEvents.length && !error && <p>No events found</p>}

      {/* RESULTS */}
      <div className="grid">
        {submitted &&
          filteredEvents.map((event) => (
            <EventCard key={event.eventId} event={event} />
          ))}
      </div>
    </div>
  );
};

export default Filter;
