import "./App.css";
import { TodosProvider } from "./contexts/TodosContexts";
import AddTodo from "./pages/AddTodo";
import TodoList from "./pages/TodoList";

function App() {
  return (
    <>
      <TodosProvider>
        <h1>Todo List</h1>
        <AddTodo />
        <TodoList />
      </TodosProvider>
    </>
  );
}

export default App;
