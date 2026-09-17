package br.edu.exemplo.ia.dto;

import java.util.UUID;

public record FuncionarioResponse(
        UUID id,
        String nome,
        UUID empresaId,
        String empresaNome,
        String empresaArea,
        String cargo,
        boolean podeGerenciarFuncionarios
) {
}