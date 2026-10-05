package br.edu.exemplo.ia.dto;

import java.util.UUID;

public record EmpresaResponse(
	UUID id,
	String name,
	String razaoSocial,
	String cnpj,
	int numeroFuncionarios,
	String cep,
	String endereco,
	String cidade,
	String estado,
	String adminResponsavel,
	String area
) {
}
