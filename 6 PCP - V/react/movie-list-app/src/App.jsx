import React, { useState } from "react";
import MovieList from "./components/MovieList.jsx";
import MovieStats from "./components/MovieStats";
import "./App.css";
import AppRouter from "./routers/AppRouter.jsx";
import { MovieProvider } from "./context/MovieContext.jsx";

const App = () => {
  return (
    <MovieProvider>
      <AppRouter />
    </MovieProvider>
  );
};

export default App;
