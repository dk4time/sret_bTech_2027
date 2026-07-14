import { Link } from "react-router-dom";

const Navbar = () => {
  return (
    <nav className="navbar">
      <Link to="/">Tasks</Link>

      <Link to="/users">Users</Link>

      <Link to="/projects">Projects</Link>
    </nav>
  );
};

export default Navbar;
