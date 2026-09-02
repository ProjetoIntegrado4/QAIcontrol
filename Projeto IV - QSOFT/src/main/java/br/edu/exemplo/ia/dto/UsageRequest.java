package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record UsageRequest(@NotNull UUID projectId, @Positive long tokens, @NotBlank String model) {
}