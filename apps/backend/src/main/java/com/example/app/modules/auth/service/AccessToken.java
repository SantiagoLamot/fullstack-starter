package com.example.app.modules.auth.service;

public record AccessToken(String value, long expiresInSeconds) {
}
