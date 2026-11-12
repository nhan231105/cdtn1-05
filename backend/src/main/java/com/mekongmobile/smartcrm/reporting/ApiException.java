package com.mekongmobile.smartcrm.reporting;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final String code;
    private final Map<String, Object> fields;

    public ApiException(String code, String message) {
        this(code, message, Map.of());
    }

    public ApiException(String code, String message, Map<String, Object> fields) {
        super(message);
        this.code = code;
        this.fields = Map.copyOf(fields);
    }

    public String getCode() { return code; }
    public Map<String, Object> getFields() { return fields; }

    public static ApiException validation(String message) {
        return new ApiException("VALIDATION_FAILED", message);
    }

    public static ApiException forbidden() {
        return new ApiException("FORBIDDEN_SCOPE", "Không có quyền truy cập dữ liệu này");
    }

    public static ApiException unauthenticated() {
        return new ApiException("UNAUTHENTICATED", "Vui lòng đăng nhập lại");
    }
}
