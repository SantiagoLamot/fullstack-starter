package com.example.app.modules.auth.dto.request;

import com.example.app.common.validation.MaxUtf8Bytes;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Password policy for a NEW password (activation, reset, change): 10 to 72 characters (BCrypt limit), at least
 * one letter and one digit, at most 72 UTF-8 bytes (accents and ñ take two). Each rule reports its own message.
 */
@NotBlank
@Size(min = 10, max = 72, message = "{auth.password.size}")
@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "{auth.password.pattern}")
@MaxUtf8Bytes(max = MaxUtf8Bytes.BCRYPT)
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidPassword {

    String message() default "{auth.password.pattern}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
