import { useNavigate } from "react-router-dom";
import { useCourses } from "../context/CourseContext";

const CourseCard = ({ course }) => {
  const navigate = useNavigate();
  const { toggleActive } = useCourses();

  return (
    <div
      className="card"
      data-testid="course-item"
      onClick={() => navigate(`/courses/${course.courseId}`)}
    >
      {/* ✅ Edge Case: Missing Title */}
      <h3>{course.title || "Untitled Course"}</h3>

      <p>Instructor: {course.instructor || "Unknown"}</p>

      <p>Category: {course.category || "General"}</p>

      <p>Price: ₹{course.price}</p>

      <p>Students: {course.enrolledStudents}</p>

      {/* ✅ Edge Case: Invalid rating */}
      {typeof course.rating === "number" && <p>Rating: {course.rating}</p>}
      <p>Status: {course.isActive ? "Active" : "Inactive"}</p>

      <button onClick={() => toggleActive(course.courseId)}>
        Toggle Active
      </button>
    </div>
  );
};

export default CourseCard;
