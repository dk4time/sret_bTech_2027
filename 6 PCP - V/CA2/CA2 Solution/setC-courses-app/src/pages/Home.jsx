const Home = () => {
  return (
    <div className="home">
      <h1>Welcome to Course Platform</h1>

      <p>Explore courses, track enrollments, and analyze performance easily.</p>

      <div className="home-cards">
        <div className="home-card">
          <h3>📚 Courses</h3>
          <p>Browse all available courses</p>
        </div>

        <div className="home-card">
          <h3>🔍 Filter</h3>
          <p>Filter courses by category or status</p>
        </div>

        <div className="home-card">
          <h3>📊 Stats</h3>
          <p>View analytics and performance</p>
        </div>
      </div>
    </div>
  );
};

export default Home;
