import { useEffect, useState } from "react";

import {
  getProjects,
  createProject,
  updateProject,
  deleteProject,
} from "../services/projectService";

import { getUsers } from "../services/userService";

const ProjectsPage = () => {
  /*
  ====================================================
  STATES
  ====================================================
  */

  const [projects, setProjects] = useState([]);

  const [users, setUsers] = useState([]);

  const [name, setName] = useState("");

  const [description, setDescription] = useState("");

  const [owner, setOwner] = useState("");

  const [members, setMembers] = useState([]);

  const [editingId, setEditingId] = useState(null);

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
  FETCH PROJECTS
  ====================================================
  */

  const fetchProjects = async () => {
    try {
      const data = await getProjects();

      setProjects(data);
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

    fetchProjects();
  }, []);

  /*
  ====================================================
  HANDLE MEMBERS
  ====================================================
  */

  const handleMembersChange = (e) => {
    const selectedMembers = Array.from(
      e.target.selectedOptions,
      (option) => option.value,
    );

    // console.log("Selected members:", selectedMembers);

    setMembers(selectedMembers);
  };

  /*
  ====================================================
  HANDLE EDIT
  ====================================================
  */

  const handleEdit = (project) => {
    setEditingId(project._id);

    setName(project.name);

    setDescription(project.description || "");

    setOwner(project.owner?._id || "");

    setMembers(project.members?.map((member) => member._id) || []);
  };

  /*
  ====================================================
  HANDLE DELETE
  ====================================================
  */

  const handleDelete = async (id) => {
    try {
      await deleteProject(id);

      fetchProjects();
    } catch (err) {
      console.log(err);
    }
  };

  /*
  ====================================================
  HANDLE SUBMIT
  ====================================================
  */

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setError("");

      const payload = {
        name,
        description,
        owner,

        members: members.map((memberId) => ({
          user: memberId,
          role: "member",
        })),
      };

      /*
      ==================================================
      UPDATE
      ==================================================
      */

      if (editingId) {
        await updateProject(editingId, payload);
      } else {
        /*
      ==================================================
      CREATE
      ==================================================
      */
        await createProject(payload);
      }

      /*
      ==================================================
      RESET
      ==================================================
      */

      setEditingId(null);

      setName("");

      setDescription("");

      setOwner("");

      setMembers([]);

      fetchProjects();
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong");
    }
  };

  return (
    <div className="projects-page">
      <h1>Projects Page</h1>

      {/* ==================================================
          FORM
      ================================================== */}

      <form onSubmit={handleSubmit} className="project-form">
        <input
          type="text"
          placeholder="Project name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />

        <textarea
          placeholder="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        {/* OWNER */}

        <select value={owner} onChange={(e) => setOwner(e.target.value)}>
          <option value="">Select Owner</option>

          {users.map((user) => (
            <option key={user._id} value={user._id}>
              {user.name}
            </option>
          ))}
        </select>

        {/* MEMBERS */}

        <select multiple value={members} onChange={handleMembersChange}>
          {users.map((user) => (
            <option key={user._id} value={user._id}>
              {user.name}
            </option>
          ))}
        </select>

        <button type="submit">
          {editingId ? "Update Project" : "Create Project"}
        </button>
      </form>

      {/* ==================================================
          ERROR
      ================================================== */}

      {error && <p className="error-message">{error}</p>}

      {/* ==================================================
          PROJECT LIST
      ================================================== */}

      <h2>Projects</h2>

      <div className="project-list">
        {projects.map((project) => (
          <div key={project._id} className="project-card">
            <h3>{project.name}</h3>

            <p>{project.description}</p>

            <p>
              <strong>Owner:</strong> {project.owner?.name}
            </p>

            <div>
              <strong>Members:</strong>

              <ul>
                {project.members.map(
                  (member) => (
                    console.log(member),
                    (
                      <li key={member.user?._id}>
                        {member.user?.name} ({member.role})
                      </li>
                    )
                  ),
                )}
              </ul>
            </div>

            {/* ==============================
                  ACTIONS
              ============================== */}

            <div className="project-actions">
              <button onClick={() => handleEdit(project)}>Edit</button>

              <button onClick={() => handleDelete(project._id)}>Delete</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProjectsPage;
