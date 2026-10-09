package br.edu.exemplo.ia.dto;

import java.util.UUID;

public record FuncionarioResponse(
        UUID id,
        String nome,
        String cpf,
        String emailCorporativo,
        Long telefone,
        String cargo,
        String setor,
        UUID empresaId,
        String empresaNome,
        String empresaArea,
        String matricula,
        String statusConta,
        UUID gestorResponsavelId,
        String gestorResponsavelNome,
        boolean podeGerenciarFuncionarios
) {
}