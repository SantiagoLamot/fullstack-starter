package com.example.app.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates every exception into an RFC 9457 ProblemDetail with a stable {@code type} (see
 * {@link ProblemType}) and a Spanish {@code title}/{@code detail}. 400 validation problems carry an
 * {@code errors: [{field, message}]} property. Also used by the security entry point / access denied
 * handler so that 401/403 share the same format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String VALIDATION_DETAIL = "La solicitud contiene datos inválidos.";
    static final String MALFORMED_BODY_DETAIL = "El cuerpo de la solicitud es inválido o está mal formado.";
    static final String INVALID_VALUE_MESSAGE = "tiene un formato inválido";

    // ---------------------------------------------------------------- application exceptions

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ProblemDetail> handleApplication(ApplicationException ex) {
        ProblemDetail problem = ProblemDetails.of(ex.problemType(), ex.getMessage());
        if (!ex.fieldErrors().isEmpty()) {
            problem.setProperty(ProblemDetails.ERRORS_PROPERTY, ex.fieldErrors());
        }
        ex.properties().forEach(problem::setProperty);
        return respond(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiFieldError> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(new ApiFieldError(leafName(violation.getPropertyPath()), violation.getMessage()));
        }
        return respond(ProblemDetails.validation(VALIDATION_DETAIL, sorted(errors)));
    }

    // ---------------------------------------------------------------- security

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(ProblemType.UNAUTHORIZED.status())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ProblemDetails.of(ProblemType.UNAUTHORIZED,
                        "Se requiere un token de acceso válido."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return respond(ProblemDetails.of(ProblemType.FORBIDDEN, "No tenés permisos para realizar esta operación."));
    }

    // ---------------------------------------------------------------- persistence

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return respond(ProblemDetails.of(ProblemType.CONFLICT,
                "El recurso fue modificado por otra persona. Recargá los datos e intentá de nuevo."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Only the constraint name is logged: the driver message contains the offending value (e.g. an email).
        log.warn("Data integrity violation on constraint {}", constraintName(ex));
        return respond(ProblemDetails.of(ProblemType.CONFLICT,
                "La operación entra en conflicto con datos existentes (por ejemplo, un valor duplicado)."));
    }

    // ---------------------------------------------------------------- fallback

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ProblemDetails.of(ProblemType.INTERNAL,
                "Ocurrió un error inesperado. Intentá de nuevo más tarde."));
    }

    // ---------------------------------------------------------------- Spring MVC exceptions

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiFieldError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.add(new ApiFieldError(error.getField(), error.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.add(new ApiFieldError(error.getObjectName(), error.getDefaultMessage())));
        return validationResponse(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiFieldError> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                for (FieldError fieldError : parameterErrors.getFieldErrors()) {
                    errors.add(new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage()));
                }
            } else {
                String name = result.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    errors.add(new ApiFieldError(name, error.getDefaultMessage()));
                }
            }
        }
        return validationResponse(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = ex.getPropertyName() != null ? ex.getPropertyName() : "request";
        return validationResponse(List.of(new ApiFieldError(field, INVALID_VALUE_MESSAGE)), headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = invalidValueField(ex);
        if (field != null) {
            return validationResponse(List.of(new ApiFieldError(field, INVALID_VALUE_MESSAGE)), headers);
        }
        ProblemDetail problem = ProblemDetails.validation(MALFORMED_BODY_DETAIL, List.of());
        problem.setType(ProblemType.BAD_REQUEST.type());
        problem.setTitle(ProblemType.BAD_REQUEST.title());
        return ResponseEntity.status(problem.getStatus()).headers(headers).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(ProblemType.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido.");
        return ResponseEntity.status(problem.getStatus()).headers(headers).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(ProblemType.NOT_FOUND, "El recurso solicitado no existe.");
        return ResponseEntity.status(problem.getStatus()).headers(headers).body(problem);
    }

    /** Normalizes the ProblemDetail of every other framework exception to our type/title catalog. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem && "about:blank".equals(String.valueOf(problem.getType()))) {
            ProblemType problemType = ProblemType.forStatus(statusCode);
            if (problemType != null) {
                problem.setType(problemType.type());
                problem.setTitle(problemType.title());
            }
            if (statusCode.is5xxServerError()) {
                log.error("Framework error with status {}", statusCode.value(), ex);
                problem.setDetail("Ocurrió un error inesperado. Intentá de nuevo más tarde.");
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ---------------------------------------------------------------- helpers

    private ResponseEntity<Object> validationResponse(List<ApiFieldError> errors, HttpHeaders headers) {
        ProblemDetail problem = ProblemDetails.validation(VALIDATION_DETAIL, sorted(errors));
        return ResponseEntity.status(problem.getStatus()).headers(headers).body(problem);
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static List<ApiFieldError> sorted(List<ApiFieldError> errors) {
        return errors.stream()
                .sorted(Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::message))
                .toList();
    }

    private static String constraintName(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }
        return "unknown";
    }

    /** Name of the JSON property whose value could not be converted (e.g. an unknown enum constant), or null. */
    private static String invalidValueField(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof tools.jackson.databind.exc.MismatchedInputException mismatch) {
            return mismatch.getPath().stream().map(tools.jackson.core.JacksonException.Reference::getPropertyName)
                    .filter(java.util.Objects::nonNull).reduce((first, second) -> first + "." + second).orElse(null);
        }
        return null;
    }

    private static String leafName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            if (node.getName() != null) {
                name = node.getName();
            }
        }
        return name != null ? name : path.toString();
    }
}
