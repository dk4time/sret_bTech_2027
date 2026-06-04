import { useEffect } from "react";
import { useCourses } from "../context/CourseContext";
import { isValidCourse } from "../utils/validateCourse";

const Stats = () => {
  const { courses } = useCourses();

  // ✅ Safety
  const safeCourses = Array.isArray(courses) ? courses : [];

  // ✅ Q1 dependency
  const validCourses = safeCourses.filter(isValidCourse);

  // 🔥 Q5 CORE LOGIC (MUST USE REDUCE)
  const stats = validCourses.reduce(
    (acc, course) => {
      // ❗ Ignore invalid isActive
      if (typeof course.isActive !== "boolean") return acc;

      acc.totalCourses++;

      if (course.isActive === true) {
        acc.activeCourses++;
      } else {
        acc.inactiveCourses++;
      }

      return acc;
    },
    {
      totalCourses: 0,
      activeCourses: 0,
      inactiveCourses: 0,
    },
  );

  // ✅ GLOBAL STATE (MANDATORY)
  useEffect(() => {
    window.appState = stats;
  }, [stats]);

  return (
    <div className="container">
      <h2>Course Analytics</h2>

      {/* ❗ VERY IMPORTANT → ONLY NUMBERS */}
      <div className="stats">
        <div>
          Total Courses:
          <span data-testid="total-courses">{stats.totalCourses}</span>
        </div>

        <div>
          Active Courses:
          <span data-testid="active-courses">{stats.activeCourses}</span>
        </div>

        <div>
          Inactive Courses:
          <span data-testid="inactive-courses">{stats.inactiveCourses}</span>
        </div>
      </div>
    </div>
  );
};

export default Stats;
