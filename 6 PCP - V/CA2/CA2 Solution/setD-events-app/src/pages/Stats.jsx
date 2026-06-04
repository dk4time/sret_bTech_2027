import { useEffect } from "react";
import { useEvents } from "../context/EventContext";
import { isValidEvent } from "../utils/validateEvent";

const Stats = () => {
  const { events } = useEvents();

  // ✅ Safety
  const safeEvents = Array.isArray(events) ? events : [];

  // ✅ Q1 dependency
  const validEvents = safeEvents.filter(isValidEvent);

  // 🔥 CORE LOGIC (MUST USE REDUCE)
  const stats = validEvents.reduce(
    (acc, e) => {
      // ❗ Only consider Active & Sold Out
      if (!["Active", "Sold Out"].includes(e.status)) return acc;

      acc.totalEvents++;

      if (e.status === "Active") acc.activeEvents++;
      if (e.status === "Sold Out") acc.soldOutEvents++;

      return acc;
    },
    {
      totalEvents: 0,
      activeEvents: 0,
      soldOutEvents: 0,
    },
  );

  // ✅ GLOBAL STATE (MANDATORY)
  useEffect(() => {
    window.appState = stats;
  }, [stats]);

  return (
    <div className="container">
      <h2>Event Analytics</h2>

      <div className="stats">
        <div>
          Total Events:
          <span data-testid="total-events">{stats.totalEvents}</span>
        </div>

        <div>
          Active Events:
          <span data-testid="active-events">{stats.activeEvents}</span>
        </div>

        <div>
          Sold Out Events:
          <span data-testid="soldout-events">{stats.soldOutEvents}</span>
        </div>
      </div>
    </div>
  );
};

export default Stats;
