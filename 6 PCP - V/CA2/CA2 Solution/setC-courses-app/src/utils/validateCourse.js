export const isValidCourse = (course) => {
  if (!course) return false;

  const hasTitle =
    typeof course.title === "string" && course.title.trim() !== "";

  const validPrice = typeof course.price === "number" && course.price > 0;

  const validStudents =
    typeof course.enrolledStudents === "number" && course.enrolledStudents > 0;

  const validActive = typeof course.isActive === "boolean";

  return hasTitle && validPrice && validStudents && validActive;
};
