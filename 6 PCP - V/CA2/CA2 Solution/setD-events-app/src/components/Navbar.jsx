import { NavLink } from "react-router-dom";
import "../App.css";

const Navbar = () => {
  return (
    <nav className="navbar">
      <h2 className="logo">Event Booking</h2>

      <div className="nav-links">
        <NavLink to="/" end>
          Home
        </NavLink>

        <NavLink to="/events">Events</NavLink>

        <NavLink to="/filter">Filter</NavLink>

        <NavLink to="/stats">Stats</NavLink>
      </div>
    </nav>
  );
};

export default Navbar;
