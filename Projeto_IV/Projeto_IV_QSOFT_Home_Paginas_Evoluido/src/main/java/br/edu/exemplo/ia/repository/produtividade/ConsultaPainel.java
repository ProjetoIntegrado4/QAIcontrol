package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Executa as consultas do painel: resolve os nomes lógicos pelo {@link EsquemaBase} e passa
 * os parâmetros comuns. Os repositórios de cada seção usam esta classe em vez do JDBC direto.
 */
@Component
public class ConsultaPainel {

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final EsquemaBase esquema;

    public ConsultaPainel(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx, EsquemaBase esquema) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.esquema = esquema;
    }

    /**
     * Executa as consultas numa transação com o JIT do PostgreSQL desligado:
     * nessas agregações a compilação JIT custa mais do que a própria consulta.
     */
    public <T> T consultar(Supplier<T> consultas) {
        return tx.execute(s -> {
            jdbc.getJdbcTemplate().execute("set local jit = off");
            return consultas.get();
        });
    }

    public List<Map<String, Object>> lista(String modelo, String empresa) {
        return jdbc.queryForList(esquema.sql(modelo), esquema.parametros(empresa));
    }

    public Map<String, Object> linha(String modelo, String empresa) {
        return jdbc.queryForMap(esquema.sql(modelo), esquema.parametros(empresa));
    }

    public <T> T valor(String modelo, String empresa, Class<T> tipo) {
        return jdbc.queryForObject(esquema.sql(modelo), esquema.parametros(empresa), tipo);
    }
}
