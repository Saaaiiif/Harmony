package com.example.harmony.services;

import com.example.harmony.DB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {

    public record CourseCardRow(int id, String title, String subjectName, String coverImagePath) {}

    public List<CourseCardRow> listPublishedCourses() throws Exception {
        return searchPublishedCourses("", null);
    }

    public List<CourseCardRow> searchPublishedCourses(String keyword, String subjectName) throws Exception {
        List<CourseCardRow> out = new ArrayList<>();

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasSubject = subjectName != null && !subjectName.isBlank();

        StringBuilder sql = new StringBuilder(
                "SELECT c.id, c.title, s.name AS subjectname, c.cover_image_path " +
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
                        rs.getString("cover_image_path")
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
