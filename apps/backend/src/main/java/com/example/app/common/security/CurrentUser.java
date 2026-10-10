package com.example.app.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id ({@code Long}) of the authenticated user, taken from the JWT {@code sub} claim.
 * Usage: {@code public MeResponse me(@CurrentUser Long userId)}. Never read the user id from body or path.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
