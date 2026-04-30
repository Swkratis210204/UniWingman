package com.example.uniwingman.model;

public class CourseItem {
    public String studentCourseId; // student_courses.id
    public String courseId;         // courses.id
    public String code;
    public String title;
    public float  ects;
    public int    semester;         // courses.semester as int (1-8)
    public String description;
    public String status;           // "passed", "in_progress", "failed"
    public Float  grade;            // null αν δεν έχει βαθμό
    public int    academicYear;     // student_courses.academic_year
    public String takenSemester;    // student_courses.Semester ("Χειμερινό"/"Εαρινό")

    public CourseItem() {}

    // Επιστρέφει human-readable πότε πάρθηκε το μάθημα
    public String getWhenTaken() {
        if (academicYear <= 0 && (takenSemester == null || takenSemester.isEmpty())) return "—";
        String year = academicYear > 0 ? academicYear + "ο Έτος" : "";
        String sem  = (takenSemester != null && !takenSemester.isEmpty()) ? takenSemester : "";
        if (!year.isEmpty() && !sem.isEmpty()) return year + " · " + sem;
        return year + sem;
    }

    // Availability: σύγκριση courses.semester με users.current_semester
    public String getAvailability(int userCurrentSemester) {
        if (status.equals("passed"))      return "Περασμένο ✓";
        if (status.equals("in_progress")) return "Σε εξέλιξη";
        if (semester <= 0)                return "";

        int diff = semester - userCurrentSemester;
        if (diff <= 0)  return "Διαθέσιμο τώρα";
        if (diff == 1)  return "Διαθέσιμο επόμενο εξάμηνο";
        return "Διαθέσιμο σε " + diff + " εξάμηνα";
    }
}