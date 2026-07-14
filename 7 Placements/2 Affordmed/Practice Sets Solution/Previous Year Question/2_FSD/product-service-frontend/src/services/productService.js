import axios from "axios";

const BASE_URL = "http://localhost:3000";

export const getProducts = async (params = {}) => {
  const response = await axios.get(`${BASE_URL}/products`, {
    params,
  });

  console.log(response.data);

  return response.data;
};
