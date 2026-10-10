package com.example.app.common.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A business rule was violated (422). */
public class BusinessException extends ApplicationException {

    public static final String CODE_PROPERTY = "code";

    private final List<ApiFieldError> fieldErrors;
    private final Map<String, Object> properties;

    public BusinessException(String message) {
        super(message);
        this.fieldErrors = List.of();
        this.properties = Map.of();
    }

    /** 422 tied to one request field: {@code errors[{field, message}]} lets clients show it under that field. */
    public BusinessException(String message, String field) {
        super(message);
        this.fieldErrors = List.of(new ApiFieldError(field, message));
        this.properties = Map.of();
    }

    /** 422 with a stable machine-readable {@code code} property (and optional extra data) next to the message. */
    public BusinessException(String message, String code, Map<String, Object> extra) {
        super(message);
        this.fieldErrors = List.of();
        Map<String, Object> all = new LinkedHashMap<>();
        all.put(CODE_PROPERTY, code);
        all.putAll(extra);
        this.properties = Map.copyOf(all);
    }

    @Override
    public List<ApiFieldError> fieldErrors() {
        return fieldErrors;
    }

    @Override
    public Map<String, Object> properties() {
        return properties;
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.BUSINESS_RULE;
    }
}
