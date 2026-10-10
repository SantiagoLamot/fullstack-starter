package com.example.app.common.exception;

/** The uploaded content exceeds the allowed size (413). */
public class PayloadTooLargeException extends ApplicationException {

    public PayloadTooLargeException(String message) {
        super(message);
    }

    @Override
    public ProblemType problemType() {
        return ProblemType.PAYLOAD_TOO_LARGE;
    }
}
