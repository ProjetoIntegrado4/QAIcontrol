package br.edu.exemplo.ia.dto;

import java.time.Instant;
import java.util.UUID;

public record UsageResponse(
        UUID id,
        UUID projectId,
        long tokens,
        String model,
        Instant occurredAt,
        String alertLevel,
        String alertMessage
) {
}
