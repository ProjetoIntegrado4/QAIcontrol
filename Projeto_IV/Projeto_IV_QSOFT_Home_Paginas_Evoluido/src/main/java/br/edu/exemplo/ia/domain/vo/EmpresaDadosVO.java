package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record EmpresaDadosVO(
        String area,
        String razaoSocial,
        String cnpj,
        Integer numeroFuncionarios,
        String cep,
        String endereco,
        String cidade,
        String estado,
        String adminResponsavel
) {
    public EmpresaDadosVO {
        requireText(area, "Área");
        boolean hasCompanyDetails = razaoSocial != null || cnpj != null || numeroFuncionarios != null
                || cep != null || endereco != null || cidade != null || estado != null || adminResponsavel != null;
        if (hasCompanyDetails) {
            requireText(razaoSocial, "Razão social");
            requireText(cnpj, "CNPJ");
            requireText(cep, "CEP");
            requireText(endereco, "Endereço");
            requireText(cidade, "Cidade");
            requireText(estado, "Estado");
            requireText(adminResponsavel, "Nome do admin responsável");
            if (numeroFuncionarios == null || numeroFuncionarios < 0) {
                throw new IllegalArgumentException("Número de funcionários inválido");
            }
        }
    }

    public EmpresaDadosVO(String area) {
        this(area, null, null, null, null, null, null, null, null);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " obrigatório");
        }
    }
}