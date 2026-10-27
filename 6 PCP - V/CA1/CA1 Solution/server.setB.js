import express from "express";
import { getToken, getDataset } from "./tokenService.js";

const app = express();
const PORT = process.env.PORT || 5050;

app.use(express.json());

let products = [];

/* ===============================
   Load Data Once (Caching)
=============================== */
const startServer = async () => {
  try {
    const response = await getToken();
    const { token, dataUrl } = response;
    const data = await getDataset(token, dataUrl);
    products = data.products;
    console.log("Dataset B loaded successfully");
    app.listen(PORT, () => {
      console.log(`Student SET B running on port ${PORT}`);
    });
  } catch (err) {
    console.log("Error loading data:", err.message);
  }
};

/* ===============================
   SET B Endpoints
=============================== */

/* Q1 – Highest Rated Product
   GET /products/top-rated
*/
app.get("/products/top-rated", (req, res) => {
  if (!products.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const topProduct = products.reduce((max, product) =>
    product.rating > max.rating ? product : max,
  );

  res.json({
    id: topProduct.id,
    name: topProduct.name,
    category: topProduct.category,
    rating: topProduct.rating,
  });
});

/* Q3 – Filter by Category
   GET /products/filter?category=VALUE
*/
app.get("/products/filter", (req, res) => {
  if (!products.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const { category } = req.query;

  if (!category)
    return res.status(400).json({
      message: "category query parameter is required",
    });

  const filtered = products.filter(
    (p) => p.category.toLowerCase() === category.toLowerCase(),
  );

  res.json(filtered);
});

/* Q4 – Discounted Price Calculation
   GET /products/discount/:id
*/
app.get("/products/discount/:id", (req, res) => {
  if (!products.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const id = Number(req.params.id);

  if (isNaN(id)) return res.status(400).json({ message: "Invalid ID" });

  const product = products.find((p) => p.id === id);

  if (!product) return res.status(404).json({ message: "Product not found" });

  const finalPrice = product.price - (product.price * product.discount) / 100;

  res.json({
    id: product.id,
    name: product.name,
    originalPrice: product.price,
    discountPercentage: product.discount,
    finalPrice: Number(finalPrice.toFixed(2)),
  });
});

/* Q5 – Inventory Summary
   GET /products/inventory/summary
*/
app.get("/products/inventory/summary", (req, res) => {
  if (!products.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const summary = products.reduce(
    (acc, product) => {
      acc.totalStock += product.stock;
      acc.totalInventoryValue += product.price * product.stock;
      return acc;
    },
    { totalStock: 0, totalInventoryValue: 0 },
  );

  res.json(summary);
});

/* Q2 – Get Product By ID
   GET /products/:id
*/
app.get("/products/:id", (req, res) => {
  if (!products.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const id = Number(req.params.id);

  if (isNaN(id)) return res.status(400).json({ message: "Invalid ID" });

  const product = products.find((p) => p.id === id);

  if (!product) return res.status(404).json({ message: "Product not found" });

  res.json(product);
});

/* Health */
app.get("/", (req, res) => {
  res.json({ message: "SET B API Running" });
});

/* =============================== */

startServer();
