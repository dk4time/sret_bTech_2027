import { useOrders } from "../context/OrderContext";
import OrderCard from "../components/OrderCard";
import { isValidOrder } from "../utils/validateOrder";

const Orders = () => {
  const { orders } = useOrders();

  // ✅ Ensure array safety
  const safeOrders = Array.isArray(orders) ? orders : [];

  // ✅ Strict validation

  const validOrders = orders.filter(isValidOrder);

  // ✅ ADD THIS (IMPORTANT)
  const pendingOrders = validOrders.filter(
    (order) => order.status !== "Delivered",
  );

  return (
    <div className="container">
      <h2>Orders</h2>

      <div className="order-list">
        {pendingOrders.map((order) => (
          <OrderCard key={order.orderId} order={order} />
        ))}
      </div>
    </div>
  );
};

export default Orders;
