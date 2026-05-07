import axios from "axios";

const BASE_URL = "http://localhost:5050/api/projects";

/*
====================================================
GET PROJECTS
====================================================
*/
export const getProjects = async () => {
  const response = await axios.get(BASE_URL);

  return response.data;
};

/*
====================================================
CREATE PROJECT
====================================================
*/
export const createProject = async (projectData) => {
  // console.log("Creating project with data:", projectData); // Debug log
  const response = await axios.post(BASE_URL, projectData);

  return response.data;
};

/*
====================================================
UPDATE PROJECT
====================================================
*/
export const updateProject = async (id, updatedData) => {
  const response = await axios.patch(`${BASE_URL}/${id}`, updatedData);

  return response.data;
};

/*
====================================================
DELETE PROJECT
====================================================
*/
export const deleteProject = async (id) => {
  const response = await axios.delete(`${BASE_URL}/${id}`);

  return response.data;
};
