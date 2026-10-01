package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminRequest(
        @NotBlank String nome,
        @NotNull UUID empresaId,
        @NotBlank String cargo
) {
}