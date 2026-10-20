package br.edu.exemplo.ia.service.produtividade.secao;

import java.util.Set;

/**
 * Uma análise do painel de produtividade. Cada implementação é um bean do Spring: para acrescentar
 * uma análise nova basta criar outra classe que implemente esta interface (e o repositório com as
 * consultas dela); o {@code ProdutividadeService} a encontra e inclui no JSON pela {@link #chave()}.
 */
public interface SecaoPainel {

    /** Nome da seção no JSON do painel (ex.: "kpis", "areas"). */
    String chave();

    /**
     * Tabelas ("uso") e colunas ("uso.status") lógicas sem as quais a seção não faz sentido.
     * Se alguma faltar no banco, a seção vem nula e o motivo vai para "indisponiveis".
     * Partes opcionais de uma seção são verificadas por ela mesma, em {@link #calcular}.
     */
    Set<String> requisitos();

    /**
     * Calcula a seção para a empresa (id) ou para todas ("ALL"). Roda dentro de uma transação de leitura.
     * Partes opcionais são verificadas com {@code disponibilidade.tem(...)}, que registra o que faltar.
     */
    Object calcular(String empresa, Disponibilidade disponibilidade);
}
