package com.mekongmobile.smartcrm.reporting;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
public class ReportingRepository {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final NamedParameterJdbcTemplate jdbc;

    public ReportingRepository(JdbcTemplate jdbc) { this.jdbc = new NamedParameterJdbcTemplate(jdbc); }

    public boolean centerExists(Long centerId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM service_center WHERE center_id = :id", Map.of("id", centerId), Long.class) > 0;
    }

    public boolean technicianExists(Long technicianId, Long centerId) {
        String sql = "SELECT COUNT(*) FROM technician WHERE technician_id = :id";
        var params = new MapSqlParameterSource("id", technicianId);
        if (centerId != null) { sql += " AND center_id = :center"; params.addValue("center", centerId); }
        return jdbc.queryForObject(sql, params, Long.class) > 0;
    }

    public Map<String, Object> byTechnician(ReportFilter filter) {
        var params = parameters(filter);
        String sql = """
                SELECT tech.technician_id, COALESCE(tech.technician_name, CONCAT('Kỹ thuật viên #', tech.technician_id)) AS technician_name,
                       COALESCE(inv.invitations_sent, 0) AS invitations_sent,
                       COALESCE(resp.responses, 0) AS responses, resp.csat_avg, resp.satisfied_rate
                FROM technician tech
                LEFT JOIN (
                    SELECT t.technician_id, COUNT(*) AS invitations_sent
                    FROM ticket t JOIN survey_invitation i ON i.ticket_id = t.ticket_id
                    WHERE t.center_id = :center AND i.sent_at >= :start AND i.sent_at < :end AND i.status IN ('DA_GUI', 'DA_PHAN_HOI')
                    GROUP BY t.technician_id
                ) inv ON inv.technician_id = tech.technician_id
                LEFT JOIN (
                    SELECT t.technician_id, COUNT(*) AS responses, ROUND(AVG(r.score), 2) AS csat_avg,
                           ROUND(SUM(CASE WHEN r.score >= 4 THEN 1 ELSE 0 END) / COUNT(*), 2) AS satisfied_rate
                    FROM ticket t JOIN survey_response r ON r.ticket_id = t.ticket_id
                    WHERE t.center_id = :center AND r.responded_at >= :start AND r.responded_at < :end
                    GROUP BY t.technician_id
                ) resp ON resp.technician_id = tech.technician_id
                WHERE tech.center_id = :center
                ORDER BY resp.csat_avg IS NULL, resp.csat_avg DESC, tech.technician_id
                """;
        var items = jdbc.query(sql, params, (rs, row) -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("technician_id", rs.getLong("technician_id"));
            item.put("technician_name", rs.getString("technician_name"));
            item.put("invitations_sent", rs.getLong("invitations_sent"));
            item.put("responses", rs.getLong("responses"));
            item.put("csat_avg", rs.getBigDecimal("csat_avg"));
            item.put("satisfied_rate", rs.getBigDecimal("satisfied_rate"));
            return item;
        });
        var result = period(filter);
        result.put("center_id", filter.centerId());
        result.put("center_name", jdbc.queryForObject("SELECT center_name FROM service_center WHERE center_id = :center", params, String.class));
        result.put("items", items);
        return result;
    }

    public Map<String, Object> byCenter(ReportFilter filter) {
        String sql = """
                SELECT t.center_id, DATE_FORMAT(r.responded_at, '%Y-%m') AS month,
                       COUNT(*) AS responses, ROUND(AVG(r.score), 2) AS csat_avg,
                       ROUND(SUM(CASE WHEN r.score >= 4 THEN 1 ELSE 0 END) / COUNT(*), 2) AS satisfied_rate,
                       SUM(CASE WHEN r.score = 1 THEN 1 ELSE 0 END) AS score1,
                       SUM(CASE WHEN r.score = 2 THEN 1 ELSE 0 END) AS score2,
                       SUM(CASE WHEN r.score = 3 THEN 1 ELSE 0 END) AS score3,
                       SUM(CASE WHEN r.score = 4 THEN 1 ELSE 0 END) AS score4,
                       SUM(CASE WHEN r.score = 5 THEN 1 ELSE 0 END) AS score5
                FROM survey_response r JOIN ticket t ON t.ticket_id = r.ticket_id
                WHERE r.responded_at >= :start AND r.responded_at < :end
                """ + (filter.centerId() == null ? "" : " AND t.center_id = :center") + " GROUP BY t.center_id, month";
        Map<String, Map<String, Object>> summaries = new LinkedHashMap<>();
        jdbc.query(sql, parameters(filter), rs -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("responses", rs.getLong("responses"));
            item.put("csat_avg", rs.getBigDecimal("csat_avg"));
            item.put("satisfied_rate", rs.getBigDecimal("satisfied_rate"));
            var distribution = new LinkedHashMap<String, Long>();
            for (int score = 1; score <= 5; score++) distribution.put(Integer.toString(score), rs.getLong("score" + score));
            item.put("distribution", distribution);
            summaries.put(rs.getLong("center_id") + ":" + rs.getString("month"), item);
        });
        List<Map<String, Object>> items = new ArrayList<>();
        for (var center : centers(filter.centerId())) {
            for (YearMonth month = YearMonth.from(filter.from()); !month.isAfter(YearMonth.from(filter.to())); month = month.plusMonths(1)) {
                var item = new LinkedHashMap<String, Object>(center);
                item.put("month", month.toString());
                Map<String, Object> summary = summaries.get(center.get("center_id") + ":" + month);
                if (summary == null) {
                    item.put("responses", 0L);
                    item.put("csat_avg", null);
                    item.put("satisfied_rate", null);
                    item.put("distribution", Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0));
                } else item.putAll(summary);
                items.add(item);
            }
        }
        var result = period(filter);
        result.put("group_by", "month");
        result.put("items", items);
        return result;
    }

    public Map<String, Object> responseRate(ReportFilter filter) {
        // Cohort by successful send month; count responses to that same cohort by the report end.
        String sql = """
                SELECT t.center_id, DATE_FORMAT(i.sent_at, '%Y-%m') AS month,
                       COUNT(*) AS invitations_sent,
                       SUM(CASE WHEN r.response_id IS NOT NULL AND r.responded_at < :end THEN 1 ELSE 0 END) AS responses
                FROM survey_invitation i JOIN ticket t ON t.ticket_id = i.ticket_id
                LEFT JOIN survey_response r ON r.ticket_id = i.ticket_id
                WHERE i.sent_at >= :start AND i.sent_at < :end AND i.status IN ('DA_GUI', 'DA_PHAN_HOI')
                """ + (filter.centerId() == null ? "" : " AND t.center_id = :center") + " GROUP BY t.center_id, month ORDER BY t.center_id, month";
        var items = jdbc.query(sql, parameters(filter), (rs, row) -> {
            var item = new LinkedHashMap<String, Object>();
            long sent = rs.getLong("invitations_sent"), responses = rs.getLong("responses");
            item.put("center_id", rs.getLong("center_id"));
            item.put("month", rs.getString("month"));
            item.put("invitations_sent", sent);
            item.put("responses", responses);
            item.put("response_rate", sent == 0 ? null : BigDecimal.valueOf(responses).divide(BigDecimal.valueOf(sent), 2, RoundingMode.HALF_UP));
            return item;
        });
        return Map.of("items", items);
    }

    public Map<String, Object> responses(ReportFilter filter, int page, int size) {
        var params = parameters(filter).addValue("limit", size).addValue("offset", ((long) page - 1) * size);
        long total = count(filter);
        return Map.of("page", page, "size", size, "total", total, "items", responseRows(filter, params));
    }

    public byte[] export(ReportFilter filter) {
        var params = parameters(filter).addValue("limit", 10_000).addValue("offset", 0);
        return ResponseWorkbook.write(responseRows(filter, params));
    }

    public List<Map<String, Object>> centers(Long centerId) {
        String sql = "SELECT center_id, center_name FROM service_center" + (centerId == null ? "" : " WHERE center_id = :center") + " ORDER BY center_id";
        return jdbc.query(sql, new MapSqlParameterSource("center", centerId), (rs, row) -> Map.of(
                "center_id", rs.getLong("center_id"), "center_name", rs.getString("center_name")));
    }

    public List<Map<String, Object>> technicians(ReportFilter filter) {
        String sql = "SELECT technician_id, center_id, COALESCE(technician_name, CONCAT('Kỹ thuật viên #', technician_id)) AS technician_name FROM technician"
                + (filter.centerId() == null ? "" : " WHERE center_id = :center") + " ORDER BY technician_id";
        return jdbc.query(sql, parameters(filter), (rs, row) -> Map.of("technician_id", rs.getLong("technician_id"),
                "center_id", rs.getLong("center_id"), "technician_name", rs.getString("technician_name")));
    }

    public long count(ReportFilter filter) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM survey_response r JOIN ticket t ON t.ticket_id = r.ticket_id " + where(filter), parameters(filter), Long.class);
    }

    private List<Map<String, Object>> responseRows(ReportFilter filter, MapSqlParameterSource params) {
        String sql = """
                SELECT r.response_id, t.ticket_code, c.center_name,
                       COALESCE(tech.technician_name, CONCAT('Kỹ thuật viên #', t.technician_id)) AS technician_name,
                       r.score, r.comment, customer.phone, r.responded_at
                FROM survey_response r JOIN ticket t ON t.ticket_id = r.ticket_id
                JOIN service_center c ON c.center_id = t.center_id
                JOIN customer ON customer.customer_id = t.customer_id
                LEFT JOIN technician tech ON tech.technician_id = t.technician_id
                """ + where(filter) + " ORDER BY r.responded_at DESC, r.response_id DESC LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, params, this::responseRow);
    }

    private Map<String, Object> responseRow(ResultSet rs, int row) throws SQLException {
        var item = new LinkedHashMap<String, Object>();
        item.put("response_id", rs.getLong("response_id"));
        item.put("ticket_code", rs.getString("ticket_code"));
        item.put("center_name", rs.getString("center_name"));
        item.put("technician_name", rs.getString("technician_name"));
        item.put("score", rs.getInt("score"));
        item.put("comment", rs.getString("comment"));
        item.put("customer_phone", maskPhone(rs.getString("phone")));
        item.put("responded_at", rs.getTimestamp("responded_at").toLocalDateTime().atZone(ZONE).toOffsetDateTime());
        return item;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "*".repeat(phone.length() - 6) + phone.substring(phone.length() - 3);
    }

    private static String where(ReportFilter filter) {
        return " WHERE r.responded_at >= :start AND r.responded_at < :end AND r.score BETWEEN :scoreMin AND :scoreMax"
                + (filter.centerId() == null ? "" : " AND t.center_id = :center")
                + (filter.technicianId() == null ? "" : " AND t.technician_id = :technician");
    }

    private static MapSqlParameterSource parameters(ReportFilter filter) {
        return new MapSqlParameterSource().addValue("center", filter.centerId()).addValue("technician", filter.technicianId())
                .addValue("start", Timestamp.valueOf(filter.from().atStartOfDay()))
                .addValue("end", Timestamp.valueOf(filter.to().plusDays(1).atStartOfDay()))
                .addValue("scoreMin", filter.scoreMin()).addValue("scoreMax", filter.scoreMax());
    }

    private static LinkedHashMap<String, Object> period(ReportFilter filter) {
        var result = new LinkedHashMap<String, Object>();
        result.put("from", filter.from());
        result.put("to", filter.to());
        return result;
    }
}
