package br.edu.exemplo.ia.repository.produtividade;

import br.edu.exemplo.ia.config.ProdutividadeTestes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Roda todas as consultas do painel contra um JDBC simulado e confere que o SQL enviado ao banco
 * ficou sem referências lógicas e só usa parâmetros que o EsquemaBase fornece.
 */
class ConsultasTest {

    private static final Pattern PARAMETRO = Pattern.compile("(?<!:):([a-z_]+)");

    private NamedParameterJdbcTemplate jdbc;
    private ConsultaPainel db;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(List.of(Map.of("id", "C01")));
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(Map.of());
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(i -> ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null));
        when(jdbc.getJdbcTemplate()).thenReturn(mock(JdbcTemplate.class));
        db = new ConsultaPainel(jdbc, tx, new EsquemaBase(ProdutividadeTestes.propriedades()));
    }

    @Test
    void todasAsConsultasDevemSairComNomesFisicosEParametrosConhecidos() throws Exception {
        Object[] repositorios = {new ResumoConsultas(db), new IndicadoresConsultas(db), new MensalConsultas(db),
                new AreasConsultas(db), new FerramentasConsultas(db), new ColaboradoresConsultas(db),
                new GovernancaConsultas(db), new EngenhariaConsultas(db)};
        int chamadas = 0;
        for (Object r : repositorios) {
            for (Method m : r.getClass().getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers())) continue;
                m.invoke(r, m.getParameterCount() == 0 ? new Object[0] : new Object[]{"C01"});
                chamadas++;
            }
        }
        assertEquals(36, chamadas);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        List<String> enviados = new ArrayList<>();
        verify(jdbc, atLeastOnce()).queryForList(sql.capture(), params.capture());
        enviados.addAll(sql.getAllValues());
        verify(jdbc, atLeastOnce()).queryForMap(sql.capture(), params.capture());
        enviados.addAll(sql.getAllValues());
        verify(jdbc, atLeastOnce()).queryForObject(sql.capture(), params.capture(), any(Class.class));
        enviados.addAll(sql.getAllValues());

        MapSqlParameterSource conhecidos = (MapSqlParameterSource) params.getValue();
        for (String s : enviados) {
            assertFalse(s.contains("${"), s);
            Matcher p = PARAMETRO.matcher(s.replaceAll("'[^']*'", "''"));
            while (p.find()) assertTrue(conhecidos.hasValue(p.group(1)), "parâmetro desconhecido :" + p.group(1));
        }
        assertTrue(enviados.stream().anyMatch(s -> s.contains("\"public\".\"ai_usage_logs\"")));
    }

    @Test
    void empresasComProjetosDevolveOsIds() {
        assertEquals(List.of("C01"), new EngenhariaConsultas(db).empresasComProjetos());
    }

    @Test
    void consultarDesligaOJitNaTransacao() {
        assertEquals(7, db.consultar(() -> 7));
        verify(jdbc.getJdbcTemplate()).execute(eq("set local jit = off"));
    }
}
