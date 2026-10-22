package br.edu.exemplo.ia.dto;

import java.util.List;

/** Regra de recomendações por faixa de risco vigente (memória da regra, RN17), com as faixas em ordem de score. */
public record RegrasRiscoResponse(
        String versaoRegra,
        String vigencia,
        String criterio,
        List<FaixaRiscoResponse> faixas
) {
}
