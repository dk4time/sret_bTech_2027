import React from "react";
import { useMovie } from "../context/MovieContext";
import MovieCard from "../components/MovieCard";

const Favorites = () => {
  const { favorites } = useMovie();

  return (
    <div className="app-container" data-testid="favorites-page">
      <h1 className="main-title" data-testid="favorites-title">
        Favorite Movies
      </h1>

      <div className="user-list" data-testid="favorites-list">
        {favorites.length === 0 ? (
          <p data-testid="no-favorites">No favorite movies</p>
        ) : (
          favorites.map((movie) => <MovieCard key={movie.id} movie={movie} />)
        )}
      </div>
    </div>
  );
};

export default Favorites;
