package br.edu.exemplo.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EmpresaRequest(
	@NotBlank String name,
	@NotBlank String razaoSocial,
	@NotBlank String cnpj,
	@NotNull @Min(0) Integer numeroFuncionarios,
	@NotBlank String cep,
	@NotBlank String endereco,
	@NotBlank String cidade,
	@NotBlank String estado,
	@NotBlank String adminResponsavel,
	@NotBlank String area
) {
}