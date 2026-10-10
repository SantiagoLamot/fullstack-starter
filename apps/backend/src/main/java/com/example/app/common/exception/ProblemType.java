package com.example.app.common.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Catalog of RFC 9457 problem types returned by the API. {@code type} is a stable URN the clients can
 * switch on; {@code title} is a short Spanish summary shown to the end user.
 */
public enum ProblemType {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "bad-request", "Solicitud inválida"),
    VALIDATION(HttpStatus.BAD_REQUEST, "validation-error", "Datos inválidos"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized", "No autenticado"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", "Acceso denegado"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "not-found", "Recurso no encontrado"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", "Método no permitido"),
    CONFLICT(HttpStatus.CONFLICT, "conflict", "Conflicto"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type", "Tipo de contenido no soportado"),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "payload-too-large", "Archivo demasiado grande"),
    BUSINESS_RULE(HttpStatus.UNPROCESSABLE_CONTENT, "business-rule", "Operación no permitida"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "too-many-requests", "Demasiados intentos"),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Error interno");

    public static final String URN_PREFIX = "urn:app:problem:";

    private final HttpStatus status;
    private final URI type;
    private final String title;

    ProblemType(HttpStatus status, String slug, String title) {
        this.status = status;
        this.type = URI.create(URN_PREFIX + slug);
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return type;
    }

    public String title() {
        return title;
    }

    /** Default problem type for a bare HTTP status (used to normalize framework errors). */
    public static ProblemType forStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 422 -> BUSINESS_RULE;
            case 429 -> TOO_MANY_REQUESTS;
            default -> status.is5xxServerError() ? INTERNAL : null;
        };
    }
}
