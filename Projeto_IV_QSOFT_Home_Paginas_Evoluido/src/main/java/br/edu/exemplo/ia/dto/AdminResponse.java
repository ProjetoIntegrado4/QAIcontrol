package br.edu.exemplo.ia.dto;

import java.util.UUID;

public record AdminResponse(
        UUID id,
        String nome,
        UUID empresaId,
        String empresaNome,
        String empresaArea,
        String cargo
) {
}