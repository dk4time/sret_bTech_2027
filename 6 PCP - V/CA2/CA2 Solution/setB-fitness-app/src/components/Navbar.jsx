import { Link, useLocation } from "react-router-dom";
import "../index.css";

const Navbar = () => {
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <h2 className="logo">Fitness Tracker</h2>

      <div className="nav-links">
        <Link className={isActive("/") ? "active" : ""} to="/">
          Home
        </Link>

        <Link
          className={isActive("/activities") ? "active" : ""}
          to="/activities"
        >
          Activities
        </Link>

        <Link className={isActive("/filter") ? "active" : ""} to="/filter">
          Filter
        </Link>

        <Link className={isActive("/stats") ? "active" : ""} to="/stats">
          Stats
        </Link>
      </div>
    </nav>
  );
};

export default Navbar;
