const OrderReducer = (state, action) => {
  switch (action.type) {
    case "SET_ORDERS":
      return {
        ...state,
        orders: action.payload,
      };

    case "MARK_DELIVERED":
      return {
        ...state,
        orders: state.orders.map((order) => {
          // ✅ Match order
          if (order.orderId === action.payload) {
            // ❌ Already delivered → no change
            if (order.status === "Delivered") {
              return order;
            }

            // ✅ Update immutably
            return { ...order, status: "Delivered" };
          }

          return order;
        }),
      };

    default:
      return state;
  }
};

export default OrderReducer;
