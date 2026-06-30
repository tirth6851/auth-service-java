package com.authplatform.dto;

import java.time.Instant;

public record MeResponse(Long id, String email, boolean verified, Instant createdAt) {}
