import AppRouter from "./router/AppRouter.jsx";
import { OrderProvider } from "./context/OrderContext.jsx";
import "./App.css";

function App() {
  return (
    <OrderProvider>
      <AppRouter />
    </OrderProvider>
  );
}

export default App;
