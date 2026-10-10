package com.example.app.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The string encoded as UTF-8 fits in {@code max} bytes (null is valid). BCrypt only accepts 72 bytes: a
 * password of 72 characters with accents or ñ is longer than that and must be a 400, not a 500.
 */
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MaxUtf8Bytes {

    /** BCrypt input limit. */
    int BCRYPT = 72;

    int max();

    String message() default "{validation.max-utf8-bytes}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
