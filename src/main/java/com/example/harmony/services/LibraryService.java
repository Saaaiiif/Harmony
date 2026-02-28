package com.example.harmony.services;

import com.example.harmony.DB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {

    public record CourseCardRow(int id, String title, String subjectName, String coverImagePath, int saves) {}

    public List<CourseCardRow> listPublishedCourses() throws Exception {
        return searchPublishedCourses("", null);
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
            // insert into saved_courses
            String insert = "INSERT INTO saved_courses (user_id, course_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, userId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }
            // increment saves count
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
            // remove from saved_courses
            String delete = "DELETE FROM saved_courses WHERE user_id = ? AND course_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(delete)) {
                ps.setInt(1, userId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }
            // decrement saves count (don't go below 0)
            String update = "UPDATE courses SET saves = GREATEST(saves - 1, 0) WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setInt(1, courseId);
                ps.executeUpdate();
            }
        }
    }

    public List<CourseCardRow> searchPublishedCourses(String keyword, String subjectName) throws Exception {
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
        sql.append("ORDER BY c.id DESC");

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (hasKeyword) ps.setString(idx++, "%" + keyword.toLowerCase() + "%");
            if (hasSubject) ps.setString(idx, subjectName);

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
}
