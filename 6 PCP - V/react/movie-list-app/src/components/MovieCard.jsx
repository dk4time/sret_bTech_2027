import { useMovie } from "../context/MovieContext";

const MovieCard = ({ movie }) => {
  const { toggleWatched, deleteMovie, toggleFavorite } = useMovie();

  return (
    <div
      className={`movie-card ${movie.watched ? "watched" : ""}`}
      data-testid="movie-item"
    >
      <div className="movie-header">
        <h3 data-testid="movie-title">
          {movie.title || "Untitled"} ({movie.year || "N/A"})
        </h3>

        <span className="genre-tag" data-testid="movie-genre">
          {movie.genre || "Unknown"}
        </span>
      </div>

      <p data-testid="movie-status">
        Status: <strong>{movie.watched ? "Watched" : "To Watch"}</strong>
      </p>

      <div className="movie-actions">
        <button
          data-testid="toggle-watched"
          onClick={() => toggleWatched(movie.id)}
        >
          {movie.watched ? "Watched 🎬" : "To Watch 🍿"}
        </button>

        <button
          data-testid="toggle-favorite"
          onClick={() => toggleFavorite(movie.id)}
        >
          {movie.favorite ? "💔" : "❤️"}
        </button>

        <button
          data-testid="delete-movie"
          onClick={() => deleteMovie(movie.id)}
          className="delete-btn"
        >
          🗑️
        </button>
      </div>
    </div>
  );
};

export default MovieCard;
