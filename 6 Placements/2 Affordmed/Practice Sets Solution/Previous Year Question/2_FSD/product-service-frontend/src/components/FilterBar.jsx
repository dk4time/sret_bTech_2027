function FilterBar({ sort, setSort, minPrice, setMinPrice }) {
  return (
    <div
      style={{
        marginBottom: "20px",
        display: "flex",
        gap: "10px",
      }}
    >
      <select value={sort} onChange={(e) => setSort(e.target.value)}>
        <option value="price">Sort By Price</option>

        <option value="rating">Sort By Rating</option>
      </select>

      <input
        type="number"
        placeholder="Min Price"
        value={minPrice}
        onChange={(e) => setMinPrice(e.target.value)}
      />
    </div>
  );
}

export default FilterBar;
