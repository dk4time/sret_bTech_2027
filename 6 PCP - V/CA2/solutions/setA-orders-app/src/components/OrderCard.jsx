import { useOrders } from "../context/OrderContext";

const OrderCard = ({ order }) => {
  const { dispatch } = useOrders();

  return (
    <div className="order-card" data-testid="order-item">
      <h3>{order.customerName || "Unknown"}</h3>
      <p>Restaurant: {order.restaurant}</p>

      {/* ✅ Show button only if NOT delivered */}
      {order.status !== "Delivered" && (
        <button
          onClick={() =>
            dispatch({
              type: "MARK_DELIVERED",
              payload: order.orderId,
            })
          }
        >
          Mark Delivered
        </button>
      )}
    </div>
  );
};

export default OrderCard;
