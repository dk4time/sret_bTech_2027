import { createContext, useContext, useReducer, useEffect } from "react";
import OrderReducer from "../reducer/OrderReducer.jsx";
import { getToken, getDataset } from "../api/api.jsx";

const OrderContext = createContext();

const initialState = {
  orders: [],
};

export const OrderProvider = ({ children }) => {
  const [state, dispatch] = useReducer(OrderReducer, initialState);

  useEffect(() => {
    const fetchData = async () => {
      const tokenRes = await getToken("20084016", "264006", "setA");
      const data = await getDataset(tokenRes.token, tokenRes.dataUrl);
      console.log("Fetched orders:", data);
      dispatch({ type: "SET_ORDERS", payload: data.orders });
    };

    fetchData();
  }, []);

  return (
    <OrderContext.Provider value={{ orders: state.orders, dispatch }}>
      {children}
    </OrderContext.Provider>
  );
};

export const useOrders = () => useContext(OrderContext);
