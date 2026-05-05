import { useEffect } from "react";
import { useOrders } from "../context/OrderContext";
import { isValidOrder } from "../utils/validateOrder";

const Stats = () => {
  const { orders } = useOrders();

  // ✅ Safe array
  const safeOrders = Array.isArray(orders) ? orders : [];

  // ✅ Reuse Q1 validation

  const validOrders = orders.filter(isValidOrder);

  // ✅ MUST USE REDUCE (IMPORTANT)
  const stats = validOrders.reduce(
    (acc, order) => {
      // ❗ Ignore missing status
      if (!order.status) return acc;

      acc.totalOrders++;

      if (order.status === "Delivered") {
        acc.deliveredOrders++;
      }

      if (order.status === "Cancelled") {
        acc.cancelledOrders++;
      }

      return acc;
    },
    {
      totalOrders: 0,
      deliveredOrders: 0,
      cancelledOrders: 0,
    },
  );

  // ✅ GLOBAL STATE (MANDATORY)
  useEffect(() => {
    window.appState = stats;
  }, [stats]);

  return (
    <div className="container">
      <h2>Order Analytics</h2>

      <div className="stats">
        <p data-testid="total-orders">Total Orders: {stats.totalOrders}</p>

        <p data-testid="delivered-orders">
          Delivered Orders: {stats.deliveredOrders}
        </p>

        <p data-testid="cancelled-orders">
          Cancelled Orders: {stats.cancelledOrders}
        </p>
      </div>
    </div>
  );
};

export default Stats;
