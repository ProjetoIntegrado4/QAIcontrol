package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Map;

/**
 * Configuração do painel de produtividade, lida de src/main/resources/produtividade.yml.
 *
 * @param schema             schema do PostgreSQL onde estão as tabelas
 * @param csvDir             pasta dos CSVs usados para preencher tabelas ausentes ou vazias
 * @param janelaRecenteDias  janela, em dias, de "licenças ociosas" e do ranking de colaboradores
 * @param limiteRanking      quantos colaboradores aparecem no ranking
 * @param centroCustoPrefixo prefixo do centro de custo do faturamento antes do id do departamento
 * @param tabelas            nome lógico da tabela -> tabela física e suas colunas (nome lógico -> nome físico)
 * @param valores            valores de status interpretados pelas consultas (ex.: uso-sucesso -> [success])
 */
@ConfigurationProperties(prefix = "produtividade")
public record ProdutividadeProperties(
        @DefaultValue("public") String schema,
        @DefaultValue("dados/csv") String csvDir,
        @DefaultValue("90") int janelaRecenteDias,
        @DefaultValue("12") int limiteRanking,
        @DefaultValue("") String centroCustoPrefixo,
        Map<String, Tabela> tabelas,
        Map<String, List<String>> valores) {

    public ProdutividadeProperties {
        tabelas = tabelas == null ? Map.of() : Map.copyOf(tabelas);
        valores = valores == null ? Map.of() : Map.copyOf(valores);
    }

    /** Tabela física e o nome físico de cada coluna lógica. */
    public record Tabela(String nome, Map<String, String> colunas) {
        public Tabela {
            colunas = colunas == null ? Map.of() : Map.copyOf(colunas);
        }
    }
}
