package br.edu.exemplo.ia.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Opções dos filtros da tela de custos, lidas das tabelas analíticas (companies, departments, employees),
 * e o primeiro e o último dia com uso registrado, para a tela sugerir o período.
 */
public record CustoOpcoesResponse(
        List<OpcaoCustoResponse> empresas,
        List<OpcaoCustoResponse> departamentos,
        List<OpcaoCustoResponse> colaboradores,
        LocalDate dataMinima,
        LocalDate dataMaxima,
        Map<String, String> indisponiveis
) {
}
