package com.mekongmobile.smartcrm.reporting;

import com.mekongmobile.smartcrm.auth.UserIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    private final ReportingRepository repository;

    public ReportingService(ReportingRepository repository) { this.repository = repository; }

    public ReportFilter scoped(UserIdentity user, ReportFilter filter) {
        Long centerId = filter.centerId();
        if ("QL_TRUNG_TAM".equals(user.role())) {
            if (centerId != null && !centerId.equals(user.centerId())) throw ApiException.forbidden();
            centerId = user.centerId();
        }
        if (centerId != null && !repository.centerExists(centerId)) {
            throw new ApiException("CENTER_NOT_FOUND", "Không tìm thấy trung tâm");
        }
        if (filter.technicianId() != null && !repository.technicianExists(filter.technicianId(), centerId)) {
            throw ApiException.validation("Kỹ thuật viên không thuộc trung tâm");
        }
        return new ReportFilter(centerId, filter.technicianId(), filter.from(), filter.to(), filter.scoreMin(), filter.scoreMax());
    }

    public Map<String, Object> byTechnician(ReportFilter filter) {
        if (filter.centerId() == null) throw ApiException.validation("Vui lòng chọn trung tâm");
        return repository.byTechnician(filter);
    }

    public Map<String, Object> byCenter(ReportFilter filter) { return repository.byCenter(filter); }
    public Map<String, Object> responseRate(ReportFilter filter) { return repository.responseRate(filter); }

    public Map<String, Object> responses(ReportFilter filter, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw ApiException.validation("Tham số phân trang không hợp lệ");
        return repository.responses(filter, page, size);
    }

    public byte[] export(ReportFilter filter) {
        if (repository.count(filter) > 10_000) throw new ApiException("EXPORT_TOO_LARGE", "Kết quả vượt 10.000 dòng; vui lòng thu hẹp bộ lọc");
        return repository.export(filter);
    }

    public List<Map<String, Object>> centers(Long centerId) { return repository.centers(centerId); }
    public List<Map<String, Object>> technicians(ReportFilter filter) { return repository.technicians(filter); }

}
