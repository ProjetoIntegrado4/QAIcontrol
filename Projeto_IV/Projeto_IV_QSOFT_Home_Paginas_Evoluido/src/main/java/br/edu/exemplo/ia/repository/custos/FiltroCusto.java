package br.edu.exemplo.ia.repository.custos;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtro já validado de uma consulta de custo.
 *
 * @param empresa          id da empresa (ex.: C01)
 * @param inicio           primeiro dia do período (inclusive)
 * @param fim              último dia do período (inclusive)
 * @param departamento     id do departamento ou nulo
 * @param colaborador      id do colaborador ou nulo
 * @param statusCobrados   valores de status que geram custo (da RegraCusto)
 * @param aplicacoesAgente aplicações consideradas agentes de IA (da RegraCusto)
 */
public record FiltroCusto(String empresa, LocalDate inicio, LocalDate fim, String departamento, String colaborador,
                          List<String> statusCobrados, List<String> aplicacoesAgente) {
}
