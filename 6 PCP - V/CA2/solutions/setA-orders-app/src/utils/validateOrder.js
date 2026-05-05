export const isValidOrder = (order) => {
  if (!Array.isArray(order.items) || order.items.length === 0) {
    return false;
  }

  // quantity check
  const validItems = order.items.every(
    (item) =>
      item &&
      typeof item.quantity === "number" &&
      item.quantity > 0 &&
      typeof item.price === "number" &&
      item.price > 0,
  );

  if (!validItems) return false;

  // totalAmount check
  if (typeof order.totalAmount !== "number" || order.totalAmount <= 0) {
    return false;
  }

  // ✅ STRICT CHECK (NEW)
  const computedTotal = order.items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0,
  );

  if (computedTotal !== order.totalAmount) {
    return false;
  }

  // ✅ VALID STATUS ONLY
  const validStatus = ["Delivered", "Cancelled", "Pending"];
  if (!validStatus.includes(order.status)) {
    return false;
  }

  return true;
};
