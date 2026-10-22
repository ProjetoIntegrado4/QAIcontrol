package br.edu.exemplo.ia.dto;

import java.util.List;

/** Faixa do Risk Score: limite inferior inclusivo; o superior só é inclusivo na última faixa (o 100). */
public record FaixaRiscoResponse(
        String nome,
        double limiteInferior,
        double limiteSuperior,
        boolean limiteSuperiorInclusivo,
        List<String> recomendacoes
) {
}
