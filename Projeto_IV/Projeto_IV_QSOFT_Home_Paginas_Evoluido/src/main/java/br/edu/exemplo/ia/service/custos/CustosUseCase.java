package br.edu.exemplo.ia.service.custos;

import br.edu.exemplo.ia.dto.CustoOpcoesResponse;
import br.edu.exemplo.ia.dto.CustoRequest;
import br.edu.exemplo.ia.dto.CustoResponse;

public interface CustosUseCase {
    /** Empresas, e para a empresa informada (opcional) seus departamentos e colaboradores, para os filtros. */
    CustoOpcoesResponse opcoes(String empresaId);

    /** Custo total de IA da empresa no período, opcionalmente de um departamento ou de um colaborador. */
    CustoResponse calcular(CustoRequest request);
}
