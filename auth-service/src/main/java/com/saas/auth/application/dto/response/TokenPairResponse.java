package com.saas.auth.application.dto.response;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public static TokenPairResponse bearer(String access, String refresh, long expiresInSeconds) {
        return new TokenPairResponse(access, refresh, "Bearer", expiresInSeconds);
    }
}
