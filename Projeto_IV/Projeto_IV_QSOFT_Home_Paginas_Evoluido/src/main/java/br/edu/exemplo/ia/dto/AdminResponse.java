package br.edu.exemplo.ia.dto;

import java.util.UUID;

public record AdminResponse(
        UUID id,
        String nome,
        Long cpf,
        String email,
        Long telefone,
        UUID empresaId,
        String empresaNome,
        String empresaArea,
        String cargo,
        String statusConta,
        String nivelAcesso
) {
}