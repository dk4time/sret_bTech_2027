import { useParams } from "react-router-dom";
import { useCourses } from "../context/CourseContext";

const CourseDetail = () => {
  const { id } = useParams();
  const { courses } = useCourses();

  // ✅ Safety
  const safeCourses = Array.isArray(courses) ? courses : [];

  // ✅ Find course
  const course = safeCourses.find((c) => String(c.courseId) === String(id));

  // ❌ Invalid ID
  if (!course) {
    return <h3>Course not found</h3>;
  }

  // ✅ Safe values
  const price = typeof course.price === "number" ? course.price : 0;

  const students =
    typeof course.enrolledStudents === "number" ? course.enrolledStudents : 0;

  // ✅ MUST COMPUTE (IMPORTANT)
  const revenue = price * students;

  if (!safeCourses.length) {
    return <p>Loading...</p>;
  }

  return (
    <div className="container">
      <h2>{course.title || "Untitled Course"}</h2>

      <p>
        <strong>Instructor:</strong> {course.instructor || "Unknown"}
      </p>

      <p>
        <strong>Category:</strong> {course.category || "General"}
      </p>

      <p>
        <strong>Price:</strong> ₹{price}
      </p>

      <p>
        <strong>Enrolled Students:</strong> {students}
      </p>

      <p>
        <strong>Duration:</strong> {course.duration || "N/A"}
      </p>

      {/* ✅ Rating edge case */}
      {typeof course.rating === "number" && (
        <p>
          <strong>Rating:</strong> {course.rating}
        </p>
      )}

      <p>
        <strong>Status:</strong> {course.isActive ? "Active" : "Inactive"}
      </p>

      {/* 🔥 CORE OUTPUT */}
      <p>
        <strong>Revenue:</strong> ₹{revenue}
      </p>
    </div>
  );
};

export default CourseDetail;
