import { Link } from "react-router-dom";

const Home = () => {
  return (
    <div className="container">
      <div className="home-card">
        <h1>Fitness Tracker Dashboard</h1>

        <p>
          Track your daily activities, monitor goals, and analyze performance.
        </p>

        <div className="home-links">
          <Link to="/activities" className="home-btn">
            View Activities
          </Link>

          <Link to="/filter" className="home-btn">
            Filter Activities
          </Link>

          <Link to="/stats" className="home-btn">
            View Stats
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Home;
