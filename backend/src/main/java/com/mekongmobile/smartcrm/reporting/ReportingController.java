package com.mekongmobile.smartcrm.reporting;

import com.mekongmobile.smartcrm.auth.AuthService;
import com.mekongmobile.smartcrm.auth.UserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
public class ReportingController {
    private final AuthService auth;
    private final ReportingService reports;

    public ReportingController(AuthService auth, ReportingService reports) { this.auth = auth; this.reports = reports; }

    @GetMapping("/api/survey-stats/by-technician")
    public Map<String, Object> byTechnician(HttpServletRequest request, @RequestParam Map<String, String> query) {
        UserIdentity user = auth.require(request.getHeader("Authorization"), "QL_TRUNG_TAM");
        return reports.byTechnician(reports.scoped(user, filter(query)));
    }

    @GetMapping("/api/survey-stats/by-center")
    public Map<String, Object> byCenter(HttpServletRequest request, @RequestParam Map<String, String> query) {
        UserIdentity user = auth.require(request.getHeader("Authorization"), "BAN_GIAM_DOC");
        if (!"month".equals(query.getOrDefault("group_by", "month"))) throw ApiException.validation("Chỉ hỗ trợ nhóm theo tháng");
        return reports.byCenter(reports.scoped(user, filter(query)));
    }

    @GetMapping("/api/survey-stats/response-rate")
    public Map<String, Object> responseRate(HttpServletRequest request, @RequestParam Map<String, String> query) {
        UserIdentity user = auth.require(request.getHeader("Authorization"), "QL_TRUNG_TAM");
        return reports.responseRate(reports.scoped(user, filter(query)));
    }

    @GetMapping("/api/survey-responses")
    public Map<String, Object> responses(HttpServletRequest request, @RequestParam Map<String, String> query) {
        UserIdentity user = auth.require(request.getHeader("Authorization"), "QL_TRUNG_TAM", "MARKETING");
        return reports.responses(reports.scoped(user, filter(query)), number(query, "page", 1), number(query, "size", 20));
    }

    @GetMapping("/api/survey-responses/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request, @RequestParam Map<String, String> query) {
        UserIdentity user = auth.require(request.getHeader("Authorization"), "MARKETING");
        ReportFilter filter = reports.scoped(user, filter(query));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"phan-hoi-khao-sat_" + filter.from().toString().substring(0, 7) + ".xlsx\"")
                .body(reports.export(filter));
    }

    @GetMapping("/api/reference/centers")
    public Map<String, Object> centers(HttpServletRequest request) {
        UserIdentity user = auth.require(request.getHeader("Authorization"));
        return Map.of("items", reports.centers("QL_TRUNG_TAM".equals(user.role()) ? user.centerId() : null));
    }

    @GetMapping("/api/reference/technicians")
    public Map<String, Object> technicians(HttpServletRequest request, @RequestParam Map<String, String> query) {
        UserIdentity user = auth.require(request.getHeader("Authorization"));
        return Map.of("items", reports.technicians(reports.scoped(user, filter(query))));
    }

    private static ReportFilter filter(Map<String, String> query) {
        try {
            return ReportFilter.of(longNumber(query, "center_id"), longNumber(query, "technician_id"),
                    date(query, "from"), date(query, "to"), optionalNumber(query, "score_min"), optionalNumber(query, "score_max"));
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw ApiException.validation("Tham số lọc không hợp lệ");
        }
    }

    private static LocalDate date(Map<String, String> query, String key) {
        if (!query.containsKey(key)) return null;
        String value = query.get(key);
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || value.startsWith("0000-")) {
            throw ApiException.validation("Ngày phải có định dạng yyyy-MM-dd");
        }
        return LocalDate.parse(value);
    }

    private static Long longNumber(Map<String, String> query, String key) {
        return query.containsKey(key) ? Long.valueOf(query.get(key)) : null;
    }

    private static Integer optionalNumber(Map<String, String> query, String key) {
        return query.containsKey(key) ? Integer.valueOf(query.get(key)) : null;
    }

    private static int number(Map<String, String> query, String key, int defaultValue) {
        try { return query.containsKey(key) ? Integer.parseInt(query.get(key)) : defaultValue; }
        catch (NumberFormatException exception) { throw ApiException.validation("Tham số phân trang không hợp lệ"); }
    }
}
