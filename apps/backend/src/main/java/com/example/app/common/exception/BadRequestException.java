package com.example.app.common.exception;

/** The request is invalid beyond Bean Validation (400). */
public class BadRequestException extends ApplicationException {

    public BadRequestException(String message) {
        super(message);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.BAD_REQUEST;
    }
}
