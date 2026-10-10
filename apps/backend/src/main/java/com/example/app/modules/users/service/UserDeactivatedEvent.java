package com.example.app.modules.users.service;

/** Published (in the same transaction) when an account is disabled, deletion-requested or anonymized. */
public record UserDeactivatedEvent(Long userId) {
}
