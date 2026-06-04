const Home = () => {
  return (
    <div className="home">
      <h1>Welcome to Event Booking System</h1>

      <p>
        Discover events, check availability, and track ticket analytics easily.
      </p>

      <div className="home-cards">
        <div className="home-card">
          <h3>🎟️ Events</h3>
          <p>Browse all available events</p>
        </div>

        <div className="home-card">
          <h3>📍 Filter</h3>
          <p>Find events by location</p>
        </div>

        <div className="home-card">
          <h3>📊 Stats</h3>
          <p>Analyze event performance</p>
        </div>
      </div>
    </div>
  );
};

export default Home;
