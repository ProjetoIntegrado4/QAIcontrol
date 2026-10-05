package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record EmpresaVO(String value) {
    public EmpresaVO {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Nome obrigatório");


    }
}
