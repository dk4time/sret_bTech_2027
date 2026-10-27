import express from "express";
import { getToken, getDataset } from "./tokenService.js";

const app = express();
const PORT = process.env.PORT || 5050;

app.use(express.json());

let orders = [];

/* ===============================
   Load Data Once (Caching)
=============================== */
const startServer = async () => {
  try {
    const response = await getToken();
    const { token, dataUrl } = response;
    const data = await getDataset(token, dataUrl);
    orders = data.orders;
    console.log("Dataset D loaded successfully");
    app.listen(PORT, () => {
      console.log(`Student SET D running on port ${PORT}`);
    });
  } catch (err) {
    console.log("Error loading data:", err.message);
  }
};

/* ===============================
   Helper – Calculate Order Total
=============================== */
const calculateOrderTotal = (order) => {
  const itemsTotal = order.items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0,
  );

  return itemsTotal + order.shippingFee;
};

/* ===============================
   Q1 – Calculate Order Total
   GET /orders/total/:orderId
=============================== */
app.get("/orders/total/:orderId", (req, res) => {
  if (!orders.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const orderId = Number(req.params.orderId);

  if (isNaN(orderId))
    return res.status(400).json({ message: "Invalid orderId" });

  const order = orders.find((o) => o.orderId === orderId);

  if (!order) return res.status(404).json({ message: "Order not found" });

  const orderTotal = calculateOrderTotal(order);

  res.json({
    orderId: order.orderId,
    customerName: order.customerName,
    orderTotal,
  });
});

/* ===============================
   Q2 – Orders by Status
   GET /orders/status/:status
=============================== */
app.get("/orders/status/:status", (req, res) => {
  if (!orders.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const statusParam = req.params.status.toLowerCase();

  const validStatuses = ["delivered", "shipped", "processing", "cancelled"];

  if (!validStatuses.includes(statusParam))
    return res.status(400).json({ message: "Invalid status" });

  const filtered = orders
    .filter((o) => o.status.toLowerCase() === statusParam)
    .map((o) => ({
      orderId: o.orderId,
      customerName: o.customerName,
      status: o.status,
    }));

  res.json(filtered);
});

/* ===============================
   Q3 – Filter by Customer Type
   GET /orders/filter?customerType=VALUE
=============================== */
app.get("/orders/filter", (req, res) => {
  if (!orders.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const { customerType } = req.query;

  if (!customerType)
    return res.status(400).json({
      message: "customerType query parameter is required",
    });

  const filtered = orders
    .filter((o) => o.customerType.toLowerCase() === customerType.toLowerCase())
    .map((o) => ({
      orderId: o.orderId,
      customerName: o.customerName,
      customerType: o.customerType,
    }));

  res.json(filtered);
});

/* ===============================
   Q4 – Revenue Summary
   GET /orders/revenue/summary
=============================== */
app.get("/orders/revenue/summary", (req, res) => {
  if (!orders.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const summary = orders.reduce(
    (acc, order) => {
      if (order.status.toLowerCase() === "cancelled") {
        acc.cancelledOrdersCount++;
      } else {
        acc.totalRevenue += calculateOrderTotal(order);
      }
      return acc;
    },
    { totalRevenue: 0, cancelledOrdersCount: 0 },
  );

  res.json(summary);
});

/* ===============================
   Q5 – Highest Value Order
   GET /orders/highest-value
=============================== */
app.get("/orders/highest-value", (req, res) => {
  if (!orders.length)
    return res.status(503).json({ message: "Data not loaded yet" });

  const highest = orders.reduce((max, order) => {
    const total = calculateOrderTotal(order);
    const maxTotal = calculateOrderTotal(max);
    return total > maxTotal ? order : max;
  });

  res.json({
    orderId: highest.orderId,
    customerName: highest.customerName,
    totalAmount: calculateOrderTotal(highest),
  });
});

/* Health */
app.get("/", (req, res) => {
  res.json({ message: "SET D API Running" });
});

/* =============================== */

startServer();
