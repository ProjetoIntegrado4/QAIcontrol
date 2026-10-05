package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank String usuario,
        @NotBlank String senha
) {
}