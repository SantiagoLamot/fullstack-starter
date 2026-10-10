package com.example.app.common.exception;

/** Authentication failed on purpose (bad credentials, invalid session). Always a generic message (401). */
public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(String message) {
        super(message);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.UNAUTHORIZED;
    }
}
