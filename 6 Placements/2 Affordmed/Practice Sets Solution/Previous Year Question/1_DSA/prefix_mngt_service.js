import express from "express";

const app = express();
const PORT = 3030;

const words = [
  "bonfire",
  "bonsai",
  "cardio",
  "case",
  "character",
  "camera",
  "cat",
  "dog",
];

function getUniquePrefix(word) {
  for (let i = 1; i <= word.length; i++) {
    const prefix = word.substring(0, i);

    let count = 0;

    for (const currentWord of words) {
      if (currentWord.startsWith(prefix)) {
        count++;
      }
    }

    if (count === 1) {
      return prefix;
    }
  }

  return word;
}

app.get("/prefixes", (req, res) => {
  const keywords = req.query.keywords;

  if (!keywords) {
    return res.status(400).json({
      message: "keywords query parameter is required",
    });
  }

  const keywordArray = keywords.split(",").map((keyword) => keyword.trim());

  const result = [];

  for (const keyword of keywordArray) {
    if (words.includes(keyword)) {
      result.push({
        keyword,
        status: "found",
        prefix: getUniquePrefix(keyword),
      });
    } else {
      result.push({
        keyword,
        status: "not_found",
        prefix: "not_applicable",
      });
    }
  }

  return res.status(200).json(result);
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
