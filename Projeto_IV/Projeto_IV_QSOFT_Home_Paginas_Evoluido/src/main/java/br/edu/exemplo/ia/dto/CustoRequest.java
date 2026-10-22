package br.edu.exemplo.ia.dto;

/**
 * Filtros da tela de Custos de IA (US PP-18), como chegam na query string.
 * Os ids são os das tabelas analíticas (ex.: C01, C01-D02, C01-E0001) e as datas vêm em AAAA-MM-DD.
 * As validações ficam no CustosService, para a API devolver mensagens claras, como "Empresa obrigatória".
 */
public record CustoRequest(
        String empresaId,
        String inicio,
        String fim,
        String departamentoId,
        String colaboradorId
) {
}
