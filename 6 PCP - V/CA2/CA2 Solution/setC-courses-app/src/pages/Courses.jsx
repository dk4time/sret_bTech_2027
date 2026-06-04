import { useCourses } from "../context/CourseContext";
import { isValidCourse } from "../utils/validateCourse";
import CourseCard from "../components/CourseCard";

const Courses = () => {
  const { courses, loading } = useCourses();

  // ✅ Safety check
  const safeCourses = Array.isArray(courses) ? courses : [];

  // ✅ Q1 CORE LOGIC (MANDATORY)
  const validCourses = safeCourses.filter(isValidCourse);

  if (loading) return <p>Loading courses...</p>;

  if (!validCourses.length) {
    return <h3>No valid courses available</h3>;
  }

  return (
    <div className="grid">
      {validCourses.map((course) => (
        <CourseCard key={course.courseId} course={course} />
      ))}
    </div>
  );
};

export default Courses;
