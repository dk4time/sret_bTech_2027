import express from "express";
import axios from "axios";

const app = express();

const PORT = 3000;

function isValidUrl(url) {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

async function fetchNumbers(url) {
  try {
    const response = await axios.get(url, {
      timeout: 500,
    });

    return response.data.numbers || [];
  } catch (error) {
    console.log(`Failed to fetch data from ${url}`);
    return [];
  }
}

app.get("/numbers", async (req, res) => {
  let urls = req.query.url;

  if (!urls) {
    return res.status(400).json({
      success: false,
      message: "url query parameter is required",
    });
  }

  if (!Array.isArray(urls)) {
    urls = [urls];
  }

  const validUrls = urls.filter(isValidUrl);

  const results = await Promise.all(validUrls.map(fetchNumbers));

  const mergedNumbers = results.flat();

  const uniqueSortedNumbers = [...new Set(mergedNumbers)].sort((a, b) => a - b);

  return res.status(200).json({
    numbers: uniqueSortedNumbers,
  });
});

app.listen(PORT, () => {
  console.log(`Server Running on Port ${PORT}`);
});
