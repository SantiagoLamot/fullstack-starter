package com.example.app.common.exception;

/** The operation conflicts with the current state, e.g. duplicated data (409). */
public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super(message);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.CONFLICT;
    }
}
