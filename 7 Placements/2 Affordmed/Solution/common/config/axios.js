import axios from "axios";

const axiosClient = axios.create({
  timeout: 500,

  headers: {
    "Content-Type": "application/json",
  },
});

export default axiosClient;
