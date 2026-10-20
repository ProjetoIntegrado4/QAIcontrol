package br.edu.exemplo.ia.service.produtividade;

import java.util.Map;

public interface ProdutividadeUseCase {
    /** Empresas, período da base, avisos de qualidade dos dados e o mapeamento em uso. */
    Map<String, Object> resumo();

    /** Indicadores do painel para uma empresa (id como C01) ou para todas (ALL). */
    Map<String, Object> painel(String empresa);
}
