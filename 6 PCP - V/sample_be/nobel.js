// nobel.js
// Simple API call example using Nobel Prize API

import express from "express";
import axios from "axios";

const app = express();
const PORT = 5050;

const BASE_URL = "https://api.nobelprize.org/2.1/nobelPrizes";

// --------------------------------------------------
// 1️⃣ Single API Call – Nobel Prize in a Year
// --------------------------------------------------

app.get("/api/nobel/:year", async (req, res) => {
  const { year } = req.params;

  try {
    //   const response = await axios.get(`${BASE_URL}?nobelPrizeYear=${year}`, {
    //     headers: {
    //       "User-Agent": "Mozilla/5.0",
    //       Accept: "application/json",
    //     },
    //   });
    const response = await axios.get(BASE_URL, {
      params: { nobelPrizeYear: year },
      headers: {
        "User-Agent": "Mozilla/5.0",
      },
    });

    res.json({
      year,
      count: response.data.nobelPrizes.length,
      data: response.data.nobelPrizes,
    });
  } catch (error) {
    res.status(500).json({
      message: "Failed to fetch Nobel Prize data",
      error: error.message,
    });
  }
});

// --------------------------------------------------
// 2️⃣ Multiple API Calls – Nobel Prize Between Years
// --------------------------------------------------

app.get("/api/nobel/range/:start/:end", async (req, res) => {
  const { start, end } = req.params;

  try {
    const promises = [];
    for (let year = Number(start); year <= Number(end); year++) {
      promises.push(
        axios.get(`${BASE_URL}?nobelPrizeYear=${year}`, {
          headers: {
            "User-Agent": `Mozilla/5.0`,
            Accept: `application/json`,
          },
        }),
      );
    }

    // for (let year = Number(start); year <= Number(end); year++) {
    //   promises.push(
    //     axios.get(BASE_URL, {
    //       params: {
    //         nobelPrizeYear: year,
    //       },
    //       headers: {
    //         "User-Agent": "Mozilla/5.0",
    //         Accept: "application/json",
    //       },
    //     }),
    //   );
    // }

    // Run all API calls in parallel
    const results = await Promise.all(promises);

    const combinedData = results.map((result, index) => ({
      year: Number(start) + index,
      count: result.data.nobelPrizes.length,
    }));

    res.json({
      range: `${start} - ${end}`,
      data: combinedData,
    });
  } catch (error) {
    res.status(500).json({
      message: "Failed to fetch Nobel Prize range data",
      error: error.message,
    });
  }
});

app.use("/", (req, res) => {
  res.json({ message: "Api is working" });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
