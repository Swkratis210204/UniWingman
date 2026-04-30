package com.example.uniwingman.model;

public class CourseItem {
    public String studentCourseId;
    public String courseId;
    public String code;
    public String title;
    public float  ects;
    public int    semester;       // πρώτο semester αν είναι "7,8"
    public String rawSemester;    // αρχική τιμή π.χ. "7,8" ή "6"
    public String description;
    public String status;
    public Float  grade;
    public int    academicYear;
    public String takenSemester;  // "Χειμερινό" ή "Εαρινό"

    public CourseItem() {}

    public String getWhenTaken() {
        if (academicYear <= 0 && (takenSemester == null || takenSemester.isEmpty())) return "—";
        String year = academicYear > 0 ? academicYear + "ο Έτος" : "";
        String sem  = (takenSemester != null && !takenSemester.isEmpty()) ? takenSemester : "";
        if (!year.isEmpty() && !sem.isEmpty()) return year + " · " + sem;
        return year + sem;
    }

    public String getAvailability(int userCurrentSemester) {
        if (status.equals("passed")) return "Περασμένο ✓";

        // Μαθήματα που προσφέρονται σε πολλά εξάμηνα (π.χ. "7,8")
        boolean isMultiSemester = rawSemester != null && rawSemester.contains(",");

        if (status.equals("in_progress")) {
            if (isMultiSemester) return "Σε εξέλιξη";
            if (semester <= 0)   return "Σε εξέλιξη";

            int diff = semester - userCurrentSemester;
            if (diff < 0) {
                // Χειμερινό (odd) → επόμενο εξάμηνο, Εαρινό (even) → Σεπτέμβριος
                if (semester % 2 != 0) return "Σε εξέλιξη · Επόμενο εξάμηνο";
                else                   return "Σε εξέλιξη · Σεπτέμβριος";
            }
            return "Σε εξέλιξη";
        }

        if (semester <= 0) return "";
        int diff = semester - userCurrentSemester;
        if (diff <= 0)  return "Διαθέσιμο τώρα";
        if (diff == 1)  return "Διαθέσιμο επόμενο εξάμηνο";
        return "Διαθέσιμο σε " + diff + " εξάμηνα";
    }
}