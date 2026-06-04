import { useState } from "react";
import { useCourses } from "../context/CourseContext";
import { isValidCourse } from "../utils/validateCourse";
import CourseCard from "../components/CourseCard";

const Filter = () => {
  const { courses } = useCourses();

  const [category, setCategory] = useState("");
  const [error, setError] = useState("");

  // ✅ Safety
  const safeCourses = Array.isArray(courses) ? courses : [];

  // ✅ Q1 Dependency
  const validCourses = safeCourses.filter(isValidCourse);

  // ✅ Q3 FILTER LOGIC
  const filteredCourses = validCourses.filter((course) =>
    course.category?.toLowerCase().includes(category.toLowerCase()),
  );

  // 🔥 HANDLE SUBMIT
  const handleFilter = (e) => {
    e.preventDefault();

    if (!category.trim()) {
      setError("Please enter a category");
      return;
    }

    setError("");
  };

  return (
    <div className="container">
      <h2>Filter Courses</h2>

      {/* INPUT */}
      <form onSubmit={handleFilter}>
        <input
          type="text"
          placeholder="Enter category"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          data-testid="filter-input"
        />

        <button type="submit">Filter</button>
      </form>

      {/* ERROR */}
      {error && <p className="error">{error}</p>}

      {/* RESULT */}
      {category && !filteredCourses.length && !error && <p>No courses found</p>}

      <div className="grid">
        {filteredCourses.map((course) => (
          <CourseCard key={course.courseId} course={course} />
        ))}
      </div>
    </div>
  );
};

export default Filter;
