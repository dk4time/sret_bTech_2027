import React, { useState, useContext } from "react";
import { MovieContext } from "../context/MovieContext";
import { v4 as uuidv4 } from "uuid";
import Favorites from "./Favorites";

const MovieForm = () => {
  const { addMovie } = useContext(MovieContext);
  const [movie, setMovie] = useState({
    title: "",
    genre: "",
    year: "",
  });

  const handleChange = (event) => {
    setMovie((prev) => ({ ...prev, [event.target.name]: event.target.value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const nameRegex = /^[A-Za-z\s]{2,}$/;
    const yearRegex = /^\d{4}$/;

    if (!nameRegex.test(movie.title) || !yearRegex.test(movie.year)) {
      alert("Please enter a valid movie title and 4-digit year");
      return;
    }

    const newMovie = {
      id: uuidv4(),
      ...movie,
      watched: false,
      favorite: false,
    };

    addMovie(newMovie);
    setMovie({ title: "", genre: "", year: "" });
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="movie-form"
      data-testid="movie-form"
    >
      <input
        type="text"
        placeholder="Movie Title"
        name="title"
        value={movie.title}
        onChange={handleChange}
        data-testid="input-title"
      />
      <input
        type="text"
        placeholder="Genre"
        name="genre"
        value={movie.genre}
        onChange={handleChange}
        data-testid="input-genre"
      />
      <input
        type="text"
        placeholder="Year"
        name="year"
        value={movie.year}
        onChange={handleChange}
        data-testid="input-year"
      />
      <button type="submit" data-testid="add-movie-btn">
        Add Movie
      </button>
    </form>
  );
};

export default MovieForm;
