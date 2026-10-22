package br.edu.exemplo.ia.dto;

import java.util.List;

/** Faixa e recomendações de uma unidade num período (RD11: unidade, período, versão, score e classificação). */
public record UnidadeRiscoResponse(
        String unidade,
        String periodo,
        Double score,
        String faixa,
        List<String> recomendacoes,
        String versaoRegra
) {
}
