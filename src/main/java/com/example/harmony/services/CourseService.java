package com.example.harmony.services;

import com.example.harmony.DB;


import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CourseService {

    public record CourseFileRow(long id, String originalName, String mimeType, long sizeBytes, Timestamp uploadedAt) {}

    public static final class CreateCourseRequest {
        private final String title;
        private final String subjectName;
        private final Integer userId;
        private final List<File> files;

        public CreateCourseRequest(String title, String subjectName, Integer userId, List<File> files) {
            this.title = title;
            this.subjectName = subjectName;
            this.userId = userId;
            this.files = (files == null) ? List.of() : List.copyOf(files);
        }

        public String title() { return title; }
        public String subjectName() { return subjectName; }
        public Integer userId() { return userId; }
        public List<File> files() { return files; }
    }

    public int createCourse(CreateCourseRequest req) throws Exception {
        Objects.requireNonNull(req, "req");

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Course title is required");

        Connection conn = null;
        try {
            conn = DB.getConnection();
            if (conn == null) throw new IllegalStateException("No DB connection");

            conn.setAutoCommit(false);

            Integer subjectId = null;
            String subjectName = req.subjectName() == null ? "" : req.subjectName().trim();
            if (!subjectName.isEmpty()) {
                subjectId = getOrCreateSubjectId(conn, subjectName);
            }

            int courseId = insertCourse(conn, title, subjectId, req.userId());

            for (File f : req.files()) {
                if (f == null) continue;
                insertCourseFile(conn, courseId, f);
            }

            conn.commit();
            return courseId;

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw ex;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void deleteCourse(int courseId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            ps.setInt(1, courseId);
            ps.executeUpdate();
        }
    }

    private int getOrCreateSubjectId(Connection conn, String subjectName) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM subject WHERE LOWER(name) = LOWER(?)"
        )) {
            ps.setString(1, subjectName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO subject(name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setString(1, subjectName);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        throw new IllegalStateException("Failed to create subject");
    }

    private int insertCourse(Connection conn, String title, Integer subjectId, Integer userId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO courses(title, subjectid, userid) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setString(1, title);

            if (subjectId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, subjectId);

            if (userId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, userId);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        throw new IllegalStateException("Failed to create course (no generated key)");
    }

    private void insertCourseFile(Connection conn, int courseId, File f) throws Exception {
        byte[] data = Files.readAllBytes(f.toPath());
        String mime = Files.probeContentType(f.toPath());
        if (mime == null) mime = "application/octet-stream";

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO coursefile(courseid, originalname, mimetype, sizebytes, filedata) VALUES (?, ?, ?, ?, ?)"
        )) {
            ps.setInt(1, courseId);
            ps.setString(2, f.getName());
            ps.setString(3, mime);
            ps.setLong(4, data.length);
            ps.setBytes(5, data);
            ps.executeUpdate();
        }
    }

    public List<com.example.harmony.model.SubjectRow> listSubjects() throws Exception {
        List<com.example.harmony.model.SubjectRow> out = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM subject ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new com.example.harmony.model.SubjectRow(rs.getInt("id"), rs.getString("name")));
        }
        return out;
    }

    public List<CourseFileRow> listCourseFiles(int courseId) throws Exception {
        List<CourseFileRow> out = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, originalname, mimetype, sizebytes, uploaded_at FROM coursefile WHERE courseid=? ORDER BY uploaded_at DESC"
             )) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CourseFileRow(
                            rs.getLong("id"),
                            rs.getString("originalname"),
                            rs.getString("mimetype"),
                            rs.getLong("sizebytes"),
                            rs.getTimestamp("uploaded_at")
                    ));
                }
            }
        }
        return out;
    }

    public void renameCourseFile(long fileId, String newName) throws Exception {
        String name = (newName == null) ? "" : newName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Name required");

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE coursefile SET originalname=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setLong(2, fileId);
            ps.executeUpdate();
        }
    }

    public void deleteCourseFile(long fileId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM coursefile WHERE id=?")) {
            ps.setLong(1, fileId);
            ps.executeUpdate();
        }
    }

    public byte[] loadCourseFileBytes(long fileId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT filedata FROM coursefile WHERE id=?")) {
            ps.setLong(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes(1);
            }
        }
        throw new IllegalArgumentException("File not found: " + fileId);
    }

    public String loadCourseFileMime(long fileId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT mimetype FROM coursefile WHERE id=?")) {
            ps.setLong(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return "application/octet-stream";
    }

    public String loadCourseFileName(long fileId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT originalname FROM coursefile WHERE id=?")) {
            ps.setLong(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return "download";
    }

    public void downloadCourseFile(long fileId, java.nio.file.Path destination) throws Exception {
        byte[] data = loadCourseFileBytes(fileId);
        java.nio.file.Files.write(destination, data);
    }

    public void uploadCourseFile(int courseId, File f) throws Exception {
        Objects.requireNonNull(f, "file");
        if (!f.exists() || !f.isFile()) throw new IllegalArgumentException("Invalid file: " + f);

        try (Connection conn = DB.getConnection()) {
            if (conn == null) throw new IllegalStateException("No DB connection");
            insertCourseFile(conn, courseId, f);
        }
    }

    public void renameCourse(int courseId, String newTitle) throws Exception {
        String t = (newTitle == null) ? "" : newTitle.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Title required");

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE courses SET title=? WHERE id=?")) {
            ps.setString(1, t);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        }
    }

    public void updateCourseSubject(int courseId, String subjectName) throws Exception {
        String s = (subjectName == null) ? "" : subjectName.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Subject required");

        try (Connection conn = DB.getConnection()) {
            if (conn == null) throw new IllegalStateException("No DB connection");

            Integer subjectId = getOrCreateSubjectId(conn, s);

            try (PreparedStatement ps = conn.prepareStatement("UPDATE courses SET subjectid=? WHERE id=?")) {
                ps.setInt(1, subjectId);
                ps.setInt(2, courseId);
                ps.executeUpdate();
            }
        }
    }

    public boolean isCoursePublished(int courseId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT is_published FROM courses WHERE id=?"
             )) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Course not found: " + courseId);
                return rs.getInt(1) != 0;
            }
        }
    }

    public void setCoursePublished(int courseId, boolean published) throws Exception {
        try (Connection conn = DB.getConnection()) {
            if (conn == null) throw new IllegalStateException("No database connection");

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE courses SET is_published=? WHERE id=?"
            )) {
                ps.setInt(1, published ? 1 : 0);
                ps.setInt(2, courseId);

                int updated = ps.executeUpdate();
                if (updated != 1) throw new IllegalArgumentException("Course not found: " + courseId);
            }
        }
    }





}
