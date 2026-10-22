package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Regra de custos de IA (US PP-18), lida da seção "custos" do application.yml e aplicada só em
 * {@code service/custos/RegraCusto}. PRELIMINAR – validar com o PO.
 *
 * @param versaoRegra         versão da regra, devolvida em toda resposta (RN17)
 * @param vigencia            data (AAAA-MM-DD) a partir da qual esta versão vale
 * @param moeda               moeda dos preços de ai_models (sem conversão de câmbio)
 * @param statusCobrados      valores de request_status que geram custo
 * @param aplicacoesAgente    valores de application considerados agentes de IA
 * @param limiteColaboradores quantos colaboradores aparecem no detalhamento
 */
@ConfigurationProperties(prefix = "custos")
public record CustosProperties(
        @DefaultValue("custos-v1-preliminar") String versaoRegra,
        String vigencia,
        @DefaultValue("USD") String moeda,
        List<String> statusCobrados,
        List<String> aplicacoesAgente,
        @DefaultValue("10") int limiteColaboradores) {

    public CustosProperties {
        statusCobrados = statusCobrados == null ? List.of() : List.copyOf(statusCobrados);
        aplicacoesAgente = aplicacoesAgente == null ? List.of() : List.copyOf(aplicacoesAgente);
    }
}
