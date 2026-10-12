package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record AIUsageVO(long tokens, String model) {
    /**
     * Valida os dados. Chamado ao criar a entidade, e não no construtor do record,
     * porque o Hibernate usa o construtor ao carregar registros já gravados no banco.
     */
    public AIUsageVO validar() {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens devem ser maiores que zero");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Modelo obrigatório");
        }
        return this;
    }
}