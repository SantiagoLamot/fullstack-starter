package com.example.app.common.exception;

/** One entry of the {@code errors} property of a 400 ProblemDetail. */
public record ApiFieldError(String field, String message) {
}
