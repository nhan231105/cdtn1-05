package com.mekongmobile.smartcrm.reporting;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public record ReportFilter(Long centerId, Long technicianId, LocalDate from, LocalDate to,
                           Integer scoreMin, Integer scoreMax) {
    public static ReportFilter of(Long centerId, Long technicianId, LocalDate from, LocalDate to,
                                  Integer scoreMin, Integer scoreMax) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        from = from == null ? today.withDayOfMonth(1) : from;
        to = to == null ? today.withDayOfMonth(today.lengthOfMonth()) : to;
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > 366) {
            throw ApiException.validation("Khoảng thời gian không hợp lệ (tối đa 366 ngày)");
        }
        if ((centerId != null && centerId <= 0) || (technicianId != null && technicianId <= 0)) {
            throw ApiException.validation("Mã trung tâm hoặc kỹ thuật viên không hợp lệ");
        }
        scoreMin = scoreMin == null ? 1 : scoreMin;
        scoreMax = scoreMax == null ? 5 : scoreMax;
        if (scoreMin < 1 || scoreMax > 5 || scoreMin > scoreMax) {
            throw ApiException.validation("Khoảng điểm không hợp lệ");
        }
        return new ReportFilter(centerId, technicianId, from, to, scoreMin, scoreMax);
    }
}
