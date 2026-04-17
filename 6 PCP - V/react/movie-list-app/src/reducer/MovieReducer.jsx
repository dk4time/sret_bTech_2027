const MovieReducer = (state, action) => {
  switch (action.type) {
    case "SET_MOVIES":
      return {
        ...state,
        movies: Array.isArray(action.payload) ? action.payload : [],
        loading: false,
      };

    case "ADD_MOVIE":
      return {
        ...state,
        movies: [...state.movies, action.payload],
      };

    case "TOGGLE_WATCHED":
      return {
        ...state,
        movies: state.movies.map((m) =>
          m.id === action.payload && typeof m.watched === "boolean"
            ? { ...m, watched: !m.watched }
            : m,
        ),
      };

    case "TOGGLE_FAVORITE":
      return {
        ...state,
        movies: state.movies.map((m) =>
          m.id === action.payload && typeof m.favorite === "boolean"
            ? { ...m, favorite: !m.favorite }
            : m,
        ),
      };

    case "DELETE_MOVIE":
      return {
        ...state,
        movies: state.movies.filter((m) => m.id !== action.payload),
      };

    case "SET_FAVORITES":
      return {
        ...state,
        favorites: state.movies.filter((m) => m.favorite === true),
      };

    default:
      console.warn("Unknown action:", action.type);
      return state;
  }
};

export default MovieReducer;
