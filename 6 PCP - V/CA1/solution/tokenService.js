import axios from "axios";

const BASE_URL = "https://t4e-testserver.onrender.com/api";

// Call Public API → Get Token
export const getToken = async () => {
  const response = await axios.post(`${BASE_URL}/public/token`, {
    studentId: "E0123001",
    set: "setE",
  });

  return response.data;
};

// Call Private API → Get Dataset A
export const getDataset = async (token, dataUrl) => {
  const response = await axios.get(`${BASE_URL}${dataUrl}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return response.data.data;
};
