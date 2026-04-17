import { useMovie } from "../context/MovieContext";

import MovieCard from "./MovieCard";

const MovieList = () => {
  const { movies, loading } = useMovie();

  if (loading) {
    return <p data-testid="loading">Loading movies...</p>;
  }

  if (movies.length === 0) {
    return (
      <p className="no-movies" data-testid="no-movies">
        No movies available
      </p>
    );
  }

  return (
    <div className="movie-list" data-testid="movie-list">
      {movies.map((movie) => (
        <MovieCard key={movie.id} movie={movie} />
      ))}
    </div>
  );
};

export default MovieList;
