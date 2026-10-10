package com.example.app.common.exception;

import java.util.List;
import org.springframework.http.ProblemDetail;

/** Factory for the ProblemDetail bodies returned by the API. */
public final class ProblemDetails {

    public static final String ERRORS_PROPERTY = "errors";

    private ProblemDetails() {
    }

    public static ProblemDetail of(ProblemType problemType, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(problemType.status(), detail);
        problem.setType(problemType.type());
        problem.setTitle(problemType.title());
        return problem;
    }

    public static ProblemDetail validation(String detail, List<ApiFieldError> errors) {
        ProblemDetail problem = of(ProblemType.VALIDATION, detail);
        problem.setProperty(ERRORS_PROPERTY, errors);
        return problem;
    }
}
