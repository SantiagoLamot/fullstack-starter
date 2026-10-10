package com.example.app.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Base class of the exceptions services throw on purpose. The message is shown to the end user as the
 * ProblemDetail {@code detail}, so it must be in Spanish and must not leak internal details.
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract ProblemType problemType();

    /** Optional per-field errors (rendered as {@code errors[{field,message}]}, same shape as a 400). */
    public List<ApiFieldError> fieldErrors() {
        return List.of();
    }

    /** Optional extra ProblemDetail properties (stable machine-readable code and data); never changes the message. */
    public Map<String, Object> properties() {
        return Map.of();
    }
}
