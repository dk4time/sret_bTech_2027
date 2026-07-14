function ProductCard({ product }) {
  return (
    <div
      style={{
        border: "1px solid #ccc",
        padding: "16px",
        marginBottom: "10px",
        borderRadius: "8px",
      }}
    >
      <h3>{product.name}</h3>

      <p>
        <strong>Price:</strong> ₹{product.price}
      </p>

      <p>
        <strong>Rating:</strong> {product.rating}
      </p>

      <p>
        <strong>Company:</strong> {product.company}
      </p>
    </div>
  );
}

export default ProductCard;
