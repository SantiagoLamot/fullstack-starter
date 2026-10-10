package com.example.app.common.exception;

/** A rate limit was reached (429). The message never reveals whether an account exists. */
public class TooManyRequestsException extends ApplicationException {

    public static final String DEFAULT_MESSAGE = "Hiciste demasiados intentos. Esperá unos minutos y volvé a probar.";

    public TooManyRequestsException() {
        super(DEFAULT_MESSAGE);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.TOO_MANY_REQUESTS;
    }
}
