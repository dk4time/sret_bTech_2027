import axios from "axios";

const BASE_URL = "http://localhost:5050/api/users";

/*
====================================================
GET USERS
====================================================
*/
export const getUsers = async () => {
  const response = await axios.get(BASE_URL);

  return response.data;
};

/*
====================================================
CREATE USER
====================================================
*/
export const createUser = async (userData) => {
  const response = await axios.post(BASE_URL, userData);

  return response.data;
};
