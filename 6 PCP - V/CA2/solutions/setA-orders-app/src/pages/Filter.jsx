import { useState } from "react";
import { useOrders } from "../context/OrderContext";
import OrderCard from "../components/OrderCard";
import { isValidOrder } from "../utils/validateOrder";

const Filter = () => {
  const { orders } = useOrders();

  const [input, setInput] = useState("");
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  // ✅ Ensure array safety
  const safeOrders = Array.isArray(orders) ? orders : [];

  // ✅ Q1 VALIDATION REUSED (VERY IMPORTANT)

  const validOrders = orders.filter(isValidOrder);

  // ✅ FILTER LOGIC (CASE INSENSITIVE)
  const filteredOrders = validOrders.filter((order) =>
    order.restaurant?.toLowerCase().includes(input.toLowerCase()),
  );

  const handleSearch = (e) => {
    e.preventDefault();

    if (!input.trim()) {
      setError("Please enter restaurant name");
      setSearched(false);
      return;
    }

    setError("");
    setSearched(true);
  };

  return (
    <div className="container">
      <h2>Filter Orders</h2>

      {/* INPUT */}
      <form onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Enter restaurant"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>

      {/* ERROR */}
      {error && <p className="error">{error}</p>}

      {/* NO RESULTS */}
      {!error && searched && filteredOrders.length === 0 && (
        <p>No results found</p>
      )}

      {/* RESULTS */}
      <div className="order-list">
        {!error &&
          searched &&
          filteredOrders.map((order) => (
            <OrderCard key={order.orderId} order={order} />
          ))}
      </div>
    </div>
  );
};

export default Filter;
