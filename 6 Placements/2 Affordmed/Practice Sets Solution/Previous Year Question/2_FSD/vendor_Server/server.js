import express from "express";
import cors from "cors";

const app = express();
app.use(cors());
const PORT = 4000;

const vendorAProducts = [
  {
    id: 1,
    name: "Laptop A",
    price: 50000,
    rating: 4.5,
    company: "Vendor A",
  },
  {
    id: 2,
    name: "Laptop B",
    price: 45000,
    rating: 4.2,
    company: "Vendor A",
  },
  {
    id: 3,
    name: "Laptop C",
    price: 60000,
    rating: 4.8,
    company: "Vendor A",
  },
];

const vendorBProducts = [
  {
    id: 4,
    name: "Laptop D",
    price: 55000,
    rating: 4.4,
    company: "Vendor B",
  },
  {
    id: 5,
    name: "Laptop E",
    price: 70000,
    rating: 4.9,
    company: "Vendor B",
  },
  {
    id: 6,
    name: "Laptop F",
    price: 40000,
    rating: 4.1,
    company: "Vendor B",
  },
];

const vendorCProducts = [
  {
    id: 7,
    name: "Laptop G",
    price: 65000,
    rating: 4.7,
    company: "Vendor C",
  },
  {
    id: 8,
    name: "Laptop H",
    price: 52000,
    rating: 4.3,
    company: "Vendor C",
  },
  {
    id: 9,
    name: "Laptop I",
    price: 48000,
    rating: 4.0,
    company: "Vendor C",
  },
];

app.get("/vendorA/products", (req, res) => {
  res.json(vendorAProducts);
});

app.get("/vendorB/products", (req, res) => {
  res.json(vendorBProducts);
});

app.get("/vendorC/products", (req, res) => {
  res.json(vendorCProducts);
});

app.listen(PORT, () => {
  console.log(`Vendor Server Running on Port ${PORT}`);
});
