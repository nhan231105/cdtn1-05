package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reporting.ApiException;
import com.mekongmobile.smartcrm.survey.dto.SurveyTemplateRequest;
import com.mekongmobile.smartcrm.survey.repository.SurveyTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SurveyTemplateService {
    private final SurveyTemplateRepository templates;

    public SurveyTemplateService(SurveyTemplateRepository templates) { this.templates = templates; }

    @Transactional(readOnly = true)
    public List<SurveyTemplateRepository.Template> list() { return templates.findAll(); }

    @Transactional(readOnly = true)
    public SurveyTemplateRepository.Template get(long id) {
        return templates.findById(id).orElseThrow(() -> notFound());
    }

    @Transactional
    public SurveyTemplateRepository.Template create(SurveyTemplateRequest request) {
        templates.lockActive();
        if (templates.nameExists(clean(request.name()), null)) throw nameExists();
        if (Boolean.TRUE.equals(request.active())) {
            templates.deactivateAll();
        }
        long id = templates.insert(clean(request.name()), clean(request.title()), clean(request.introText()), request.active());
        return get(id);
    }

    @Transactional
    public SurveyTemplateRepository.Template update(long id, SurveyTemplateRequest request) {
        SurveyTemplateRepository.Template current = templates.lockActive().orElse(null);
        SurveyTemplateRepository.Template target = templates.lockById(id).orElseThrow(() -> notFound());
        if (templates.nameExists(clean(request.name()), id)) throw nameExists();
        boolean active = request.active();
        if (active && (current == null || current.templateId() != id)) {
            templates.deactivateAll();
        } else if (!active && target.active() && current != null && current.templateId() == id) {
            throw new ApiException("ACTIVE_TEMPLATE_REQUIRED", "Hãy kích hoạt mẫu khác trước khi tắt mẫu này");
        }
        templates.update(id, clean(request.name()), clean(request.title()), clean(request.introText()), active);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        templates.lockActive();
        SurveyTemplateRepository.Template target = templates.lockById(id).orElseThrow(() -> notFound());
        if (target.active()) throw new ApiException("ACTIVE_TEMPLATE_DELETE", "Không thể xóa mẫu đang hoạt động");
        templates.delete(id);
    }

    private static String clean(String value) { return value.trim(); }
    private static ApiException notFound() { return new ApiException("TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu khảo sát"); }
    private static ApiException nameExists() { return new ApiException("TEMPLATE_NAME_EXISTS", "Tên mẫu khảo sát đã được sử dụng"); }
}
