package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AdminRequest(
        @NotBlank String nome,
        @NotBlank String cpf,
        @NotBlank @Email String email,
        @NotNull @Positive Long telefone,
        @NotBlank String cargo,
        @NotNull UUID empresaId,
        @NotBlank String senha,
        @NotBlank String confirmacaoSenha,
        @NotBlank String statusConta,
        @NotBlank String nivelAcesso
) {
}