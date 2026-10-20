package br.edu.exemplo.ia.repository.produtividade;

import br.edu.exemplo.ia.config.ProdutividadeProperties;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduz o modelo lógico do painel para o banco real, conforme produtividade.yml.
 *
 * <p>As consultas são escritas com referências lógicas: {@code ${uso}} vira a tabela física
 * qualificada pelo schema e {@code ${uso.funcionario}} vira o nome físico da coluna. Os valores de status
 * entram como parâmetros nomeados ({@code :uso_sucesso}), junto com {@code :emp}, {@code :janela_dias}
 * e {@code :centro_custo_prefixo}.
 */
@Component
public class EsquemaBase {

    private static final Pattern REFERENCIA = Pattern.compile("\\$\\{([a-z_]+)(?:\\.([a-z_]+))?}");

    private final ProdutividadeProperties props;
    private final Map<String, String> sqlResolvido = new ConcurrentHashMap<>();

    public EsquemaBase(ProdutividadeProperties props) {
        this.props = props;
    }

    public String schema() {
        return props.schema();
    }

    public int janelaRecenteDias() {
        return props.janelaRecenteDias();
    }

    /** Nome físico da tabela lógica (sem schema). */
    public String tabela(String logica) {
        ProdutividadeProperties.Tabela t = props.tabelas().get(logica);
        if (t == null || t.nome() == null) throw new IllegalArgumentException("Tabela não mapeada em produtividade.yml: " + logica);
        return t.nome();
    }

    /** Nome físico da coluna lógica. */
    public String coluna(String tabela, String coluna) {
        tabela(tabela);
        String fisica = props.tabelas().get(tabela).colunas().get(coluna);
        if (fisica == null) throw new IllegalArgumentException("Coluna não mapeada em produtividade.yml: " + tabela + "." + coluna);
        return fisica;
    }

    /** Tabelas lógicas mapeadas, com o nome físico de cada uma. */
    public Map<String, String> tabelasMapeadas() {
        Map<String, String> m = new java.util.TreeMap<>();
        props.tabelas().forEach((k, v) -> m.put(k, v.nome()));
        return m;
    }

    /** Substitui as referências lógicas da consulta pelos identificadores físicos, já entre aspas. */
    public String sql(String modelo) {
        return sqlResolvido.computeIfAbsent(modelo, m -> {
            Matcher r = REFERENCIA.matcher(m);
            StringBuilder sb = new StringBuilder();
            while (r.find()) {
                String id = r.group(2) == null
                        ? q(schema()) + "." + q(tabela(r.group(1)))
                        : q(coluna(r.group(1), r.group(2)));
                r.appendReplacement(sb, Matcher.quoteReplacement(id));
            }
            r.appendTail(sb);
            return sb.toString();
        });
    }

    /** Parâmetros comuns a todas as consultas: empresa, janela, prefixo e cada lista de valores. */
    public MapSqlParameterSource parametros(String empresa) {
        MapSqlParameterSource p = new MapSqlParameterSource("emp", empresa)
                .addValue("janela_dias", props.janelaRecenteDias())
                .addValue("limite_ranking", props.limiteRanking())
                .addValue("centro_custo_prefixo", props.centroCustoPrefixo());
        props.valores().forEach((k, v) -> p.addValue(k.replace('-', '_'), v.isEmpty() ? List.of("") : v));
        return p;
    }

    /** Valores configurados para um status (ex.: treinamento-concluido). */
    public List<String> valores(String chave) {
        return props.valores().getOrDefault(chave, List.of());
    }

    public Map<String, List<String>> todosValores() {
        return props.valores();
    }

    /**
     * Requisitos lógicos ("uso" ou "uso.status") traduzidos para "tabela" ou "tabela.coluna" físicos.
     * Requisitos que nem estão mapeados voltam como estão, marcados como não mapeados.
     */
    List<String[]> fisicos(Collection<String> requisitos) {
        return requisitos.stream().map(req -> {
            String[] partes = req.split("\\.", 2);
            try {
                return partes.length == 1
                        ? new String[]{tabela(partes[0]), null}
                        : new String[]{tabela(partes[0]), coluna(partes[0], partes[1])};
            } catch (IllegalArgumentException e) {
                return new String[]{null, req};
            }
        }).toList();
    }

    private static String q(String identificador) {
        return "\"" + identificador.replace("\"", "\"\"") + "\"";
    }
}
