import { useEffect, useState } from "react";

import ProductCard from "../components/ProductCard";
import FilterBar from "../components/FilterBar";
import Pagination from "../components/Pagination";

import { getProducts } from "../services/productService";

function Products() {
  const [products, setProducts] = useState([]);

  const [sort, setSort] = useState("price");

  const [minPrice, setMinPrice] = useState("");

  const [page, setPage] = useState(1);

  const [totalPages, setTotalPages] = useState(1);

  async function loadProducts() {
    try {
      const data = await getProducts({
        sort,
        minPrice,
        page,
        limit: 3,
      });

      setProducts(data.products);

      setTotalPages(data.totalPages);
    } catch (error) {
      console.log(error);
    }
  }

  useEffect(() => {
    loadProducts();
  }, [sort, minPrice, page]);

  return (
    <div
      style={{
        width: "80%",
        margin: "auto",
      }}
    >
      <h1>Product Catalog</h1>

      <FilterBar
        sort={sort}
        setSort={setSort}
        minPrice={minPrice}
        setMinPrice={setMinPrice}
      />

      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}

      <Pagination page={page} setPage={setPage} totalPages={totalPages} />
    </div>
  );
}

export default Products;
