import { useEffect, useState } from "react";

import {
  getTasks,
  createTask,
  updateTask,
  deleteTask,
  assignTask,
  unassignTask,
} from "../services/taskService";

import { getUsers } from "../services/userService";

import { getProjects } from "../services/projectService";

const TasksPage = () => {
  /*
  ====================================================
  DATA STATES
  ====================================================
  */

  const [tasks, setTasks] = useState([]);

  const [users, setUsers] = useState([]);

  const [projects, setProjects] = useState([]);

  /*
  ====================================================
  FORM STATES
  ====================================================
  */

  const [editingId, setEditingId] = useState(null);

  const [title, setTitle] = useState("");

  const [description, setDescription] = useState("");

  const [status, setStatus] = useState("todo");

  const [dueDate, setDueDate] = useState("");

  const [assignedTo, setAssignedTo] = useState("");

  const [project, setProject] = useState("");

  /*
  ====================================================
  FILTER STATES
  ====================================================
  */

  const [search, setSearch] = useState("");

  const [statusFilter, setStatusFilter] = useState("");

  const [projectFilter, setProjectFilter] = useState("");

  const [overdueFilter, setOverdueFilter] = useState(false);

  /*
  ====================================================
  PAGINATION STATES
  ====================================================
  */

  const [page, setPage] = useState(1);

  const [limit] = useState(5);

  const [totalPages, setTotalPages] = useState(1);

  /*
  ====================================================
  ERROR
  ====================================================
  */

  const [error, setError] = useState("");

  /*
  ====================================================
  FETCH TASKS
  ====================================================
  */

  const fetchTasks = async () => {
    try {
      const data = await getTasks({
        page,
        limit,
        search,
        status: statusFilter,
        projectId: projectFilter,
        overdue: overdueFilter,
      });

      setTasks(data.data);

      setTotalPages(data.totalPages);
    } catch (err) {
      console.log(err);
    }
  };

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
  FETCH ON FILTER CHANGE
  ====================================================
  */

  useEffect(() => {
    fetchTasks();
  }, [page, search, statusFilter, projectFilter, overdueFilter]);

  /*
  ====================================================
  HANDLE EDIT
  ====================================================
  */

  const handleEdit = (task) => {
    setEditingId(task._id);

    setTitle(task.title);

    setDescription(task.description || "");

    setStatus(task.status || "todo");

    setDueDate(task.dueDate ? task.dueDate.split("T")[0] : "");

    setAssignedTo(task.assignedTo?._id || "");

    setProject(task.project?._id || "");
  };

  /*
  ====================================================
  HANDLE DELETE
  ====================================================
  */

  const handleDelete = async (id) => {
    try {
      await deleteTask(id);

      fetchTasks();
    } catch (err) {
      console.log(err);
    }
  };

  /*
  ====================================================
  HANDLE ASSIGN
  ====================================================
  */

  const handleAssign = async (taskId, userId) => {
    try {
      await assignTask(taskId, userId);

      fetchTasks();
    } catch (err) {
      setError(err.response?.data?.message || "Assignment failed");
    }
  };

  /*
  ====================================================
  HANDLE UNASSIGN
  ====================================================
  */

  const handleUnassign = async (taskId) => {
    try {
      await unassignTask(taskId);

      fetchTasks();
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
        title,
        description,
        status,
        dueDate,
        assignedTo,
        project,
      };

      /*
      ==================================================
      UPDATE
      ==================================================
      */

      if (editingId) {
        await updateTask(editingId, payload);
      } else {
        /*
      ==================================================
      CREATE
      ==================================================
      */
        await createTask(payload);
      }

      /*
      ==================================================
      RESET
      ==================================================
      */

      setEditingId(null);

      setTitle("");

      setDescription("");

      setStatus("todo");

      setDueDate("");

      setAssignedTo("");

      setProject("");

      fetchTasks();
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong");
    }
  };

  return (
    <div className="tasks-page">
      <h1>Tasks Page</h1>

      {/* ==================================================
          FILTERS
      ================================================== */}

      <div className="task-filters">
        {/* SEARCH */}

        <input
          type="text"
          placeholder="Search tasks"
          value={search}
          onChange={(e) => {
            setPage(1);

            setSearch(e.target.value);
          }}
        />

        {/* STATUS FILTER */}

        <select
          value={statusFilter}
          onChange={(e) => {
            setPage(1);

            setStatusFilter(e.target.value);
          }}
        >
          <option value="">All Status</option>

          <option value="todo">Todo</option>

          <option value="in-progress">In Progress</option>

          <option value="done">Done</option>
        </select>

        {/* PROJECT FILTER */}

        <select
          value={projectFilter}
          onChange={(e) => {
            setPage(1);

            setProjectFilter(e.target.value);
          }}
        >
          <option value="">All Projects</option>

          {projects.map((project) => (
            <option key={project._id} value={project._id}>
              {project.name}
            </option>
          ))}
        </select>

        {/* OVERDUE */}

        <label>
          <input
            type="checkbox"
            checked={overdueFilter}
            onChange={(e) => {
              setPage(1);

              setOverdueFilter(e.target.checked);
            }}
          />
          Overdue
        </label>
      </div>

      {/* ==================================================
          FORM
      ================================================== */}

      <form onSubmit={handleSubmit} className="task-form">
        <input
          type="text"
          placeholder="Task title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        <textarea
          placeholder="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="todo">Todo</option>

          <option value="in-progress">In Progress</option>

          <option value="done">Done</option>
        </select>

        <input
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
        />

        {/* PROJECT */}

        <select value={project} onChange={(e) => setProject(e.target.value)}>
          <option value="">Select Project</option>

          {projects.map((project) => (
            <option key={project._id} value={project._id}>
              {project.name}
            </option>
          ))}
        </select>

        {/* ASSIGNED USER */}

        <select
          value={assignedTo}
          onChange={(e) => setAssignedTo(e.target.value)}
        >
          <option value="">Unassigned</option>

          {users.map((user) => (
            <option key={user._id} value={user._id}>
              {user.name}
            </option>
          ))}
        </select>

        <button type="submit">
          {editingId ? "Update Task" : "Create Task"}
        </button>
      </form>

      {/* ==================================================
          ERROR
      ================================================== */}

      {error && <p className="error-message">{error}</p>}

      {/* ==================================================
          TASK TABLE
      ================================================== */}

      <table className="task-table">
        <thead>
          <tr>
            <th>Title</th>

            <th>Status</th>

            <th>Due Date</th>

            <th>Project</th>

            <th>Assigned To</th>

            <th>Assignment</th>

            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {tasks.map((task) => (
            <tr key={task._id}>
              <td>{task.title}</td>

              <td>{task.status}</td>

              <td>{task.dueDate?.split("T")[0]}</td>

              <td>{task.project?.name}</td>

              <td>{task.assignedTo?.name || "Unassigned"}</td>

              {/* ==========================
                    ASSIGNMENT
                ========================== */}

              <td>
                <div className="assign-box">
                  <select
                    defaultValue=""
                    onChange={(e) => {
                      if (!e.target.value) return;

                      handleAssign(task._id, e.target.value);
                    }}
                  >
                    <option value="">Assign</option>

                    {users.map((user) => (
                      <option key={user._id} value={user._id}>
                        {user.name}
                      </option>
                    ))}
                  </select>

                  <button onClick={() => handleUnassign(task._id)}>
                    Unassign
                  </button>
                </div>
              </td>

              {/* ==========================
                    ACTIONS
                ========================== */}

              <td>
                <div className="table-actions">
                  <button onClick={() => handleEdit(task)}>Edit</button>

                  <button onClick={() => handleDelete(task._id)}>Delete</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* ==================================================
          PAGINATION
      ================================================== */}

      <div className="pagination">
        <button disabled={page === 1} onClick={() => setPage(page - 1)}>
          Previous
        </button>

        <span>
          Page {page} of {totalPages}
        </span>

        <button
          disabled={page === totalPages}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
};

export default TasksPage;
