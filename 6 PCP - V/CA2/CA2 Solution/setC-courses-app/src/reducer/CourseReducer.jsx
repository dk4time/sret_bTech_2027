const CourseReducer = (state, action) => {
  switch (action.type) {
    case "SET_COURSES":
      return {
        ...state,
        courses: action.payload,
        loading: false,
      };

    case "TOGGLE_ACTIVE":
      return {
        ...state,
        courses: state.courses.map((c) => {
          if (c.courseId !== action.payload) return c;

          // ❗ Business Rule (IMPORTANT)
          if (c.enrolledStudents >= 1000) {
            return { ...c, isActive: true };
          }

          return { ...c, isActive: !c.isActive };
        }),
      };

    default:
      return state;
  }
};

export default CourseReducer;
