import React, { useEffect } from "react";
import { useMovie } from "../context/MovieContext";

const MovieStats = () => {
  const { movies } = useMovie();

  const total = movies.length;

  const watchedCount = movies.filter((m) => m.watched === true).length;

  const favoriteCount = movies.filter((m) => m.favorite === true).length;

  const genreCount = movies.reduce((acc, movie) => {
    if (!movie.genre) return acc;
    acc[movie.genre] = (acc[movie.genre] || 0) + 1;
    return acc;
  }, {});

  useEffect(() => {
    window.appState = {
      totalMovies: total,
      watchedMovies: watchedCount,
      favoriteMovies: favoriteCount,
    };
  }, [movies]);

  if (!movies.length)
    return <h3 data-testid="no-stats">No movies available</h3>;

  return (
    <div className="movie-stats fade-in" data-testid="stats-page">
      <h2>Movie Stats</h2>

      <p data-testid="total-movies">Total Movies: {total}</p>
      <p data-testid="watched-movies">Watched: {watchedCount}</p>
      <p data-testid="favorite-movies">Favorite: {favoriteCount}</p>

      <div>
        <p>Genres:</p>
        <ul data-testid="genre-list">
          {Object.entries(genreCount).map(([genre, count]) => (
            <li key={genre}>
              {genre}: {count}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default MovieStats;
