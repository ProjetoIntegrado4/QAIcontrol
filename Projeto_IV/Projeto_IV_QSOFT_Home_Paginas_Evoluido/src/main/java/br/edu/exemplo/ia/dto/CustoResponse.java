package br.edu.exemplo.ia.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resultado do cálculo de custos de IA (US PP-18), com a memória de cálculo (RN17): versão e vigência
 * da regra, fórmula, status cobrados e aplicações tratadas como agente.
 * {@code total} é nulo quando as tabelas não existem no banco (motivo em {@code indisponiveis}).
 * Usos cujo modelo não tem preço não entram no total e são contados em {@code usosSemPreco} (RN19).
 */
public record CustoResponse(
        BigDecimal total,
        String moeda,
        long usosCobrados,
        long usosSemPreco,
        long tokensEntrada,
        long tokensSaida,
        BigDecimal totalAgentes,
        BigDecimal totalUsoDireto,
        List<CustoItemResponse> porDepartamento,
        List<CustoItemResponse> porColaborador,
        List<CustoItemResponse> porFerramenta,
        String versaoRegra,
        String vigencia,
        String formula,
        List<String> statusCobrados,
        List<String> aplicacoesAgente,
        String mensagem,
        Map<String, String> indisponiveis
) {
}
