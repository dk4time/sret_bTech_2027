import { useParams } from "react-router-dom";
import { useEvents } from "../context/EventContext";
import { useState } from "react";

const EventDetail = () => {
  const { id } = useParams();
  const { events, updateBooking } = useEvents();

  const [input, setInput] = useState("");
  const [error, setError] = useState("");

  const event = events.find((e) => String(e.eventId) === String(id));

  if (!event) return <h3>Event not found</h3>;

  const available = event.ticketsAvailable || 0;
  const sold = event.ticketsSold || 0;

  const handleUpdate = () => {
    const value = Number(input);

    if (isNaN(value)) {
      setError("Enter a valid number");
      return;
    }

    if (value <= sold) {
      setError("Must be greater than current sold tickets");
      return;
    }

    if (value > available) {
      setError("Cannot exceed available tickets");
      return;
    }

    setError("");

    updateBooking(event.eventId, value);
    setInput("");
  };

  return (
    <div className="container">
      <h2>{event.eventName}</h2>

      <p>
        Tickets: {sold}/{available}
      </p>
      <p>Status: {event.status}</p>

      {/* 🔥 UPDATE SECTION */}
      <div style={{ marginTop: "20px" }}>
        <input
          type="number"
          placeholder="Enter tickets sold"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />

        <button onClick={handleUpdate}>Update Booking</button>

        {error && <p className="error">{error}</p>}
      </div>
    </div>
  );
};

export default EventDetail;
