package com.example.app.common.exception;

/** The authenticated user may not perform this operation on this resource (403). */
public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String message) {
        super(message);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.FORBIDDEN;
    }
}
