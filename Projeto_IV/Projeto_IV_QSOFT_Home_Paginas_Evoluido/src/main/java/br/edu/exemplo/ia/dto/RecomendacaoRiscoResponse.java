package br.edu.exemplo.ia.dto;

import java.util.List;

/**
 * Faixa de risco de dependência de IA de um score e as recomendações dessa faixa (PS-290 / RF27), com a versão e
 * a vigência da regra (RN17). Sem score, {@code faixa} é DADOS_INSUFICIENTES, {@code recomendacoes} vem vazia e
 * {@code mensagem} explica o motivo (RN19).
 */
public record RecomendacaoRiscoResponse(
        Double score,
        String faixa,
        List<String> recomendacoes,
        String versaoRegra,
        String vigencia,
        String mensagem
) {
}
