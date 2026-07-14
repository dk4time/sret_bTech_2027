import axios from "axios";

const BASE_URL = "http://localhost:5050/api/tasks";

/*
====================================================
GET TASKS
----------------------------------------------------
Supports:
- pagination
- filters
- search
====================================================
*/
export const getTasks = async (params = {}) => {
  const response = await axios.get(BASE_URL, {
    params,
  });

  return response.data;
};

/*
====================================================
GET TASK BY ID
====================================================
*/
export const getTaskById = async (id) => {
  const response = await axios.get(`${BASE_URL}/${id}`);

  return response.data;
};

/*
====================================================
CREATE TASK
====================================================
*/
export const createTask = async (taskData) => {
  const response = await axios.post(BASE_URL, taskData);

  return response.data;
};

/*
====================================================
PATCH TASK
----------------------------------------------------
Partial update
====================================================
*/
export const updateTask = async (id, updatedData) => {
  const response = await axios.patch(`${BASE_URL}/${id}`, updatedData);

  return response.data;
};

/*
====================================================
PUT TASK
----------------------------------------------------
Full replacement
====================================================
*/
export const replaceTask = async (id, taskData) => {
  const response = await axios.put(`${BASE_URL}/${id}`, taskData);

  return response.data;
};

/*
====================================================
DELETE TASK
====================================================
*/
export const deleteTask = async (id) => {
  const response = await axios.delete(`${BASE_URL}/${id}`);

  return response.data;
};

/*
====================================================
ASSIGN TASK
====================================================
*/
export const assignTask = async (taskId, userId) => {
  const response = await axios.patch(`${BASE_URL}/${taskId}/assign`, {
    userId,
  });

  return response.data;
};

/*
====================================================
UNASSIGN TASK
====================================================
*/
export const unassignTask = async (taskId) => {
  const response = await axios.patch(`${BASE_URL}/${taskId}/unassign`);

  return response.data;
};

/*
====================================================
GET TASK STATS
====================================================
*/
export const getTaskStats = async () => {
  const response = await axios.get(`${BASE_URL}/stats`);

  return response.data;
};

/*
====================================================
GET TASK STATS BY USER
====================================================
*/
export const getTaskStatsByUser = async (userId) => {
  const response = await axios.get(`${BASE_URL}/stats/user/${userId}`);

  return response.data;
};

/*
====================================================
GET RECENT TASKS
====================================================
*/
export const getRecentTasks = async () => {
  const response = await axios.get(`${BASE_URL}/recent`);

  return response.data;
};

/*
====================================================
GET DUE SOON TASKS
====================================================
*/
export const getDueSoonTasks = async () => {
  const response = await axios.get(`${BASE_URL}/due-soon`);

  return response.data;
};

/*
====================================================
GET OVERDUE TASKS
====================================================
*/
export const getOverdueTasks = async () => {
  const response = await axios.get(`${BASE_URL}/overdue/list`);

  return response.data;
};

/*
====================================================
GET TASKS DUE ON DATE
====================================================
*/
export const getTasksDueOnDate = async (date) => {
  const response = await axios.get(`${BASE_URL}/due/${date}`);

  return response.data;
};

/*
====================================================
GET TASKS BY USER
====================================================
*/
export const getTasksByUser = async (userId) => {
  const response = await axios.get(`${BASE_URL}/user/${userId}`);

  return response.data;
};

/*
====================================================
GET UNASSIGNED TASKS
====================================================
*/
export const getUnassignedTasks = async () => {
  const response = await axios.get(`${BASE_URL}/unassigned/list`);

  return response.data;
};

/*
====================================================
ADVANCED FILTER
====================================================
*/
export const getAdvancedFilteredTasks = async (userId) => {
  const response = await axios.get(`${BASE_URL}/filter`, {
    params: {
      userId,
    },
  });

  return response.data;
};
