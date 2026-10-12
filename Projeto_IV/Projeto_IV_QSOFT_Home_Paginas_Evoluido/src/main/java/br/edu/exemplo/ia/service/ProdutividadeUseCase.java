package br.edu.exemplo.ia.service;

import java.util.Map;

public interface ProdutividadeUseCase {
    /** Empresas, período da base e avisos de qualidade dos dados. */
    Map<String, Object> resumo();

    /** Indicadores do painel para uma empresa (id como C01) ou para todas (ALL). */
    Map<String, Object> painel(String empresa);
}
