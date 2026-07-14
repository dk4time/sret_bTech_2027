import { useEffect, useState } from "react";

import { getUsers, createUser } from "../services/userService";

const UsersPage = () => {
  /*
  ====================================================
  STATES
  ====================================================
  */

  const [users, setUsers] = useState([]);

  const [name, setName] = useState("");

  const [email, setEmail] = useState("");

  const [error, setError] = useState("");

  /*
  ====================================================
  FETCH USERS
  ====================================================
  */

  const fetchUsers = async () => {
    try {
      const data = await getUsers();

      setUsers(data);
    } catch (err) {
      console.log(err);
    }
  };

  /*
  ====================================================
  PAGE LOAD
  ====================================================
  */

  useEffect(() => {
    fetchUsers();
  }, []);

  /*
  ====================================================
  CREATE USER
  ====================================================
  */

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setError("");

      await createUser({
        name,
        email,
      });

      setName("");
      setEmail("");

      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong");
    }
  };

  return (
    <div className="users-page">
      <h1>Users Page</h1>

      {/* ==================================================
          FORM
      ================================================== */}

      <form onSubmit={handleSubmit} className="user-form">
        <input
          type="text"
          placeholder="Enter name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />

        <input
          type="email"
          placeholder="Enter email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <button type="submit">Create User</button>
      </form>

      {/* ==================================================
          ERROR
      ================================================== */}

      {error && <p className="error-message">{error}</p>}

      {/* ==================================================
          USERS LIST
      ================================================== */}

      <h2>Users</h2>

      <div className="user-list">
        {users.map((user) => (
          <div key={user._id} className="user-card">
            <p>
              <strong>Name:</strong> {user.name}
            </p>

            <p>
              <strong>Email:</strong> {user.email}
            </p>

            <p>
              <strong>ID:</strong> {user._id}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default UsersPage;
