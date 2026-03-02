package services.LibraryServices;

import com.example.harmony.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReportService {

    /**
     * Returns true if the user has already submitted a report for this course.
     */
    public boolean hasReported(int reporterId, int courseId) throws Exception {
        String sql = "SELECT COUNT(*) FROM course_reports WHERE reporter_id = ? AND course_id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * Submits a report. Returns false if the user already reported this course.
     *
     * @param reporterId  ID of the user submitting the report
     * @param courseId    ID of the course being reported
     * @param reason      Short reason label (e.g. "Inappropriate content")
     * @param details     Optional extra details from the user (may be null/blank)
     */
    public boolean submitReport(int reporterId, int courseId, String reason, String details) throws Exception {
        if (hasReported(reporterId, courseId)) return false;

        String sql = "INSERT INTO course_reports (course_id, reporter_id, reason, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, reporterId);
            ps.setString(3, reason);
            ps.setString(4, details != null && !details.isBlank() ? details.trim() : null);
            ps.executeUpdate();
        }
        return true;
    }
}