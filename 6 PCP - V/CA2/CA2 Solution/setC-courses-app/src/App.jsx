import AppRouter from "./router/AppRouter.jsx";
import "./App.css";
import { CourseProvider } from "./context/CourseContext.jsx";

function App() {
  return (
    <CourseProvider>
      <AppRouter />
    </CourseProvider>
  );
}

export default App;
