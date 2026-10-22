package br.edu.exemplo.ia.dto;

/**
 * Risk Score de uma unidade num período (RD11), para o lote de recomendações. A unidade é o que o score mede
 * (hoje, no notebook, colaborador × mês: ex.: C01-E0001 e 2026-07). Score nulo significa dados insuficientes.
 */
public record UnidadeRiscoRequest(
        String unidade,
        String periodo,
        Double score
) {
}
