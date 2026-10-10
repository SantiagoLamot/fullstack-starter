package com.example.app.common.exception;

/** The requested resource does not exist (404). */
public class NotFoundException extends ApplicationException {

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.NOT_FOUND;
    }
}
