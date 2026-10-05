package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record AIUsageVO(long tokens, String model) {
    public AIUsageVO {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens devem ser maiores que zero");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Modelo obrigatório");
        }
    }
}