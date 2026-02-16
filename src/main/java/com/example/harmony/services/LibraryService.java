package com.example.harmony.services;

import com.example.harmony.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {

    public record CourseCardRow(int id, String title, String subjectName) {}

    public List<CourseCardRow> listPublishedCourses() throws Exception {
        List<CourseCardRow> out = new ArrayList<>();

        try (Connection conn = DB.getConnection()) {
            if (conn == null) throw new IllegalStateException("No DB connection");

            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT c.id,
                       c.title,
                       s.name AS subject_name
                FROM courses c
                LEFT JOIN subject s ON s.id = c.subjectid
                WHERE c.is_published = 1
                ORDER BY c.id DESC
            """);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    out.add(new CourseCardRow(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("subject_name")
                    ));
                }
            }
        }

        return out;
    }
}
