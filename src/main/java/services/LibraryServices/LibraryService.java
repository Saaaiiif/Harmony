package services.LibraryServices;

import com.example.harmony.DB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {

    public record CourseCardRow(int id, String title, String subjectName, String coverImagePath, int saves) {}

    public List<CourseCardRow> listPublishedCourses() throws Exception {
        return searchPublishedCourses("", null, "Newest");
    }

    public boolean isCourseSaved(int userId, int courseId) throws Exception {
        String sql = "SELECT COUNT(*) FROM saved_courses WHERE user_id = ? AND course_id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public boolean saveCourse(int userId, int courseId) throws Exception {
        if (isCourseSaved(userId, courseId)) return false;

        try (Connection conn = DB.getConnection()) {
            String insert = "INSERT INTO saved_courses (user_id, course_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, userId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }
            String update = "UPDATE courses SET saves = saves + 1 WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setInt(1, courseId);
                ps.executeUpdate();
            }
        }
        return true;
    }

    public void unsaveCourse(int userId, int courseId) throws Exception {
        try (Connection conn = DB.getConnection()) {
            String delete = "DELETE FROM saved_courses WHERE user_id = ? AND course_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(delete)) {
                ps.setInt(1, userId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }
            String update = "UPDATE courses SET saves = GREATEST(saves - 1, 0) WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setInt(1, courseId);
                ps.executeUpdate();
            }
        }
    }

    public List<CourseCardRow> searchPublishedCourses(String keyword, String subjectName, String sort) throws Exception {
        List<CourseCardRow> out = new ArrayList<>();

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasSubject = subjectName != null && !subjectName.isBlank();

        StringBuilder sql = new StringBuilder(
                "SELECT c.id, c.title, s.name AS subjectname, c.cover_image_path, c.saves " +
                        "FROM courses c LEFT JOIN subject s ON s.id = c.subjectid " +
                        "WHERE c.is_published = 1 "
        );

        if (hasKeyword) sql.append("AND LOWER(c.title) LIKE ? ");
        if (hasSubject) sql.append("AND LOWER(s.name) = LOWER(?) ");
        if (sort == null) sort = "Newest";
        switch (sort) {
            case "Most Saved" -> sql.append("ORDER BY c.saves DESC");
            case "Relevant"   -> sql.append("ORDER BY CASE WHEN LOWER(c.title) LIKE ? THEN 0 ELSE 1 END, c.saves DESC");
            default           -> sql.append("ORDER BY c.id DESC"); // Newest
        }

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (hasKeyword) ps.setString(idx++, "%" + keyword.toLowerCase() + "%");
            if (hasSubject) ps.setString(idx++, subjectName);
            // Relevant sort adds a second keyword bind for the CASE expression
            if ("Relevant".equals(sort)) {
                ps.setString(idx++, hasKeyword ? "%" + keyword.toLowerCase() + "%" : "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(new CourseCardRow(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("subjectname"),
                        rs.getString("cover_image_path"),
                        rs.getInt("saves")
                ));
            }
        }
        return out;
    }

    /** Returns true if the user has saved at least one course. */
    public boolean userHasSavedCourses(int userId) throws Exception {
        String sql = "SELECT COUNT(*) FROM saved_courses WHERE user_id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public List<String> listPublishedSubjects() throws Exception {
        List<String> out = new ArrayList<>();
        String sql =
                "SELECT DISTINCT s.name " +
                        "FROM subject s " +
                        "INNER JOIN courses c ON c.subjectid = s.id " +
                        "WHERE c.is_published = 1 " +
                        "ORDER BY s.name";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    /**
     * Returns up to {@code limit} published courses whose subject matches the subjects
     * the user has interacted with (courses they own OR have saved).
     * Courses the user already owns are excluded from the results.
     *
     * Logic:
     *   1. Collect subject IDs from courses the user created (user_id = ?)
     *   2. Union with subject IDs from courses the user saved (via saved_courses)
     *   3. Find published courses in those subjects that the user does NOT own
     *   4. Order by saves DESC so the most popular come first
     */
    public List<CourseCardRow> getRecommendedCourses(int userId, int limit) throws Exception {
        List<CourseCardRow> out = new ArrayList<>();

        // First, collect subject IDs from courses the user has saved.
        // If none exist yet (new user), we fall back to showing the most popular courses overall.
        List<Integer> savedSubjectIds = new ArrayList<>();
        String subjectSql =
                "SELECT DISTINCT c2.subjectid " +
                        "FROM saved_courses sc " +
                        "JOIN courses c2 ON c2.id = sc.course_id " +
                        "WHERE sc.user_id = ? AND c2.subjectid IS NOT NULL";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(subjectSql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) savedSubjectIds.add(rs.getInt(1));
        }

        // Build the main query: subject-matched if we have signals, otherwise top popular
        String sql;
        if (savedSubjectIds.isEmpty()) {
            // No save history — recommend the most popular published courses
            sql =
                    "SELECT c.id, c.title, s.name AS subjectname, c.cover_image_path, c.saves " +
                            "FROM courses c " +
                            "LEFT JOIN subject s ON s.id = c.subjectid " +
                            "WHERE c.is_published = 1 " +
                            "ORDER BY c.saves DESC " +
                            "LIMIT ?";
        } else {
            // Has save history — recommend by matching subjects, excluding already-saved
            String placeholders = savedSubjectIds.stream()
                    .map(id -> "?")
                    .collect(java.util.stream.Collectors.joining(", "));
            sql =
                    "SELECT c.id, c.title, s.name AS subjectname, c.cover_image_path, c.saves " +
                            "FROM courses c " +
                            "LEFT JOIN subject s ON s.id = c.subjectid " +
                            "WHERE c.is_published = 1 " +
                            "  AND c.id NOT IN (SELECT course_id FROM saved_courses WHERE user_id = ?) " +
                            "  AND c.subjectid IN (" + placeholders + ") " +
                            "ORDER BY c.saves DESC " +
                            "LIMIT ?";
        }

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (savedSubjectIds.isEmpty()) {
                ps.setInt(1, limit);
            } else {
                ps.setInt(1, userId); // NOT IN saved_courses
                int idx = 2;
                for (int sid : savedSubjectIds) ps.setInt(idx++, sid);
                ps.setInt(idx, limit);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(new CourseCardRow(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("subjectname"),
                        rs.getString("cover_image_path"),
                        rs.getInt("saves")
                ));
            }
        }
        return out;
    }
}