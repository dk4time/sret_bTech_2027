import { useParams } from "react-router-dom";
import { useOrders } from "../context/OrderContext";

const OrderDetail = () => {
  const { id } = useParams();
  const { orders } = useOrders();

  // ✅ Safe array check
  const safeOrders = Array.isArray(orders) ? orders : [];

  // ✅ Find order
  const order = safeOrders.find((o) => o.orderId === Number(id));

  // ❌ Invalid ID
  if (!order) {
    return <h3>Order not found</h3>;
  }

  // ✅ Safe items handling
  const items = Array.isArray(order.items) ? order.items : [];

  return (
    <div className="container">
      <h2>Order Detail</h2>

      <p>
        <strong>Order ID:</strong> {order.orderId}
      </p>
      <p>
        <strong>Customer:</strong> {order.customerName || "Unknown"}
      </p>
      <p>
        <strong>Restaurant:</strong> {order.restaurant || "N/A"}
      </p>

      <h3>Items</h3>

      {items.length === 0 ? (
        <p>No items available</p>
      ) : (
        <ul>
          {items.map((item, index) => {
            // ✅ Safe calculation
            const price = typeof item.price === "number" ? item.price : 0;

            const quantity =
              typeof item.quantity === "number" ? item.quantity : 0;

            const subtotal = price * quantity;

            return (
              <li key={index}>
                {item.name || "Unnamed Item"} - Subtotal: {subtotal}
              </li>
            );
          })}
        </ul>
      )}

      <p>
        <strong>Total Amount:</strong>{" "}
        {typeof order.totalAmount === "number" ? order.totalAmount : "N/A"}
      </p>
    </div>
  );
};

export default OrderDetail;
