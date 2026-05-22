package io.aether.api.error;

public record ErrorResponse(int status, String error, String message, String path) {}
