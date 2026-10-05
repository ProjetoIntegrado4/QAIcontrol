package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record FuncionarioRequest(
        @NotBlank String nome,
        @NotNull @Positive Long cpf,
        @NotBlank @Email String emailCorporativo,
        @NotNull @Positive Long telefone,
        @NotBlank String cargo,
        @NotBlank String setor,
        @NotNull UUID empresaId,
        @NotBlank String matricula,
        @NotBlank String senha,
        @NotBlank String statusConta,
        UUID gestorResponsavelId
) {
}