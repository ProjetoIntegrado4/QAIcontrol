package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Regra de recomendações por faixa de risco de dependência de IA (Task PS-290 / RF27, US PP-75), lida da seção
 * "risco" do application.yml e aplicada só em {@code service/risco/RegraRecomendacaoRisco}, que também a valida
 * na subida. PRELIMINAR – validar com o PO: o documento do projeto ainda não define as faixas nem os textos.
 *
 * @param versaoRegra   versão da regra, devolvida em toda resposta (RN17)
 * @param vigencia      data (AAAA-MM-DD) a partir da qual esta versão vale
 * @param faixas        faixas do Risk Score (0 a 100), cada uma com limite inferior inclusivo e superior exclusivo
 * @param recomendacoes nome da faixa -> recomendações apresentadas para ela
 */
@ConfigurationProperties(prefix = "risco")
public record RiscoProperties(
        String versaoRegra,
        String vigencia,
        List<Faixa> faixas,
        Map<String, List<String>> recomendacoes) {

    public RiscoProperties {
        faixas = faixas == null ? List.of() : List.copyOf(faixas);
        recomendacoes = recomendacoes == null ? Map.of() : Map.copyOf(recomendacoes);
    }

    /** Faixa do Risk Score. A última faixa inclui o 100. */
    public record Faixa(String nome, Double limiteInferior, Double limiteSuperior) {
    }
}
