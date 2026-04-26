package com.saas.auth.application.dto.response;

public record LoginResponse(TokenPairResponse tokens, UserResponse user) {}
