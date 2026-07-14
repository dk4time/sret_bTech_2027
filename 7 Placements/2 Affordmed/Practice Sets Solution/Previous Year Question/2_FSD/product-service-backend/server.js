import express from "express";
import axios from "axios";
import cors from "cors";

const app = express();

app.use(cors());

const PORT = 3000;

app.get("/products", async (req, res) => {
  try {
    const {
      sort,
      order = "asc",
      minPrice,
      maxPrice,
      page = 1,
      limit = 5,
    } = req.query;

    console.log("Products API Hit");

    const results = await Promise.all([
      axios.get("http://localhost:4000/vendorA/products"),
      axios.get("http://localhost:4000/vendorB/products"),
      axios.get("http://localhost:4000/vendorC/products"),
    ]);

    console.log(results);

    let products = results.flatMap((response) => response.data);

    // FILTERING

    if (minPrice) {
      products = products.filter(
        (product) => product.price >= Number(minPrice),
      );
    }

    if (maxPrice) {
      products = products.filter(
        (product) => product.price <= Number(maxPrice),
      );
    }

    // SORTING

    if (sort) {
      products.sort((a, b) => {
        if (order === "desc") {
          return b[sort] - a[sort];
        }

        return a[sort] - b[sort];
      });
    }

    // PAGINATION

    const startIndex = (Number(page) - 1) * Number(limit);

    const endIndex = startIndex + Number(limit);

    const paginatedProducts = products.slice(startIndex, endIndex);

    return res.status(200).json({
      totalProducts: products.length,
      currentPage: Number(page),
      limit: Number(limit),
      totalPages: Math.ceil(products.length / Number(limit)),
      products: paginatedProducts,
    });
  } catch (error) {
    console.log(error);

    return res.status(500).json({
      success: false,
      message: error.message,
    });
  }
});

app.listen(PORT, () => {
  console.log(`Product Service Running on Port ${PORT}`);
});
