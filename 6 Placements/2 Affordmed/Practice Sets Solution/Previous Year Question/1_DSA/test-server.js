import express from "express";

const app = express();

app.get("/primes", (req, res) => {
  res.json({
    numbers: [2, 3, 5, 7, 11, 13, 17],
  });
});

app.get("/fibo", (req, res) => {
  res.json({
    numbers: [1, 1, 2, 3, 5, 8, 13, 21],
  });
});

app.get("/odd", (req, res) => {
  res.json({
    numbers: [1, 3, 5, 7, 9, 11, 13],
  });
});

app.get("/rand", (req, res) => {
  res.json({
    numbers: [4, 7, 10, 13, 16, 19],
  });
});

app.listen(4000, () => {
  console.log("Test Server Running On Port 4000");
});
