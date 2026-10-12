package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record EmpresaName(String value) {
    /**
     * Valida os dados. Chamado ao criar a entidade, e não no construtor do record,
     * porque o Hibernate usa o construtor ao carregar registros já gravados no banco.
     */
    public EmpresaName validar() {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Nome obrigatório");
        return this;
    }
}
