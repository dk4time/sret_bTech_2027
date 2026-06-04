import AppRouter from "./routers/AppRouter";
import { EventProvider } from "./context/EventContext";

const App = () => {
  return (
    <EventProvider>
      <AppRouter />
    </EventProvider>
  );
};

export default App;
