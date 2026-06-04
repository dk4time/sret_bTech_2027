import AppRouter from "./router/AppRouter.jsx";
import "./App.css";
import { ActivityProvider } from "./context/ActivityContext.jsx";

function App() {
  return (
    <ActivityProvider>
      <AppRouter />
    </ActivityProvider>
  );
}

export default App;
