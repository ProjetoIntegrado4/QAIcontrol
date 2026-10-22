package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Registro de uso de IA. {@code funcionarioId} é OPCIONAL (US PP-21): quando vem preenchido, o uso só é registrado
 * se o colaborador tiver aceitado as políticas obrigatórias vigentes da empresa. Sem ele, nada muda.
 */
public record UsageRequest(
        @NotNull UUID empresaId,
        @Positive long tokens,
        @NotBlank String model,
        UUID funcionarioId
) {
    public UsageRequest(UUID empresaId, long tokens, String model) {
        this(empresaId, tokens, model, null);
    }
}
