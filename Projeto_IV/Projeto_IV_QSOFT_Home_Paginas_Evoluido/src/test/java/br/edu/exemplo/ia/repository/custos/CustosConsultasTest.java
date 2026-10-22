package br.edu.exemplo.ia.repository.custos;

import br.edu.exemplo.ia.config.ProdutividadeTestes;
import br.edu.exemplo.ia.repository.produtividade.EsquemaBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Consultas de custo contra um JDBC simulado: o SQL enviado ao banco precisa sair com os nomes físicos
 * do produtividade.yml, agregado no banco (SUM/GROUP BY) e só com parâmetros que a consulta fornece.
 */
class CustosConsultasTest {

    private static final Pattern PARAMETRO = Pattern.compile("(?<!:):([a-z_]+)");
    private static final FiltroCusto FILTRO = new FiltroCusto("C01", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
            "C01-D01", null, List.of("success"), List.of("Pipeline Interno", "API Direta"));

    private NamedParameterJdbcTemplate jdbc;
    private CustosConsultas consultas;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class))).thenReturn(List.of(Map.of("id", "C01")));
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(Map.of("inicio", "2025-06-01"));
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class))).thenReturn(true);
        consultas = new CustosConsultas(jdbc, new EsquemaBase(ProdutividadeTestes.propriedades()));
    }

    private void conferirSql(String sql, MapSqlParameterSource params) {
        assertFalse(sql.contains("${"), sql);
        Matcher p = PARAMETRO.matcher(sql.replaceAll("'[^']*'", "''"));
        while (p.find()) assertTrue(params.hasValue(p.group(1)), "parâmetro desconhecido :" + p.group(1) + " em " + sql);
    }

    @Test
    void usoDeveSerAgregadoNoBancoPorDimensaoEModelo() {
        for (Agrupamento a : Agrupamento.values()) assertEquals(List.of(Map.of("id", "C01")), consultas.uso(FILTRO, a));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc, atLeastOnce()).queryForList(sql.capture(), params.capture());
        assertEquals(Agrupamento.values().length, sql.getAllValues().size());

        for (int i = 0; i < sql.getAllValues().size(); i++) {
            String s = sql.getAllValues().get(i);
            MapSqlParameterSource p = (MapSqlParameterSource) params.getAllValues().get(i);
            conferirSql(s, p);
            assertTrue(s.contains("\"public\".\"ai_usage_logs\""), s);
            assertTrue(s.contains("\"model_id\""), s);
            assertTrue(s.contains("\"application\" in (:aplicacoes_agente)"), s);
            assertTrue(s.contains("\"request_status\" in (:status_cobrados)"), s);
            assertTrue(s.contains("sum("), s);
            assertTrue(s.contains("group by"), s);

            assertEquals("C01", p.getValue("emp"));
            assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), p.getValue("inicio"));
            assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), p.getValue("fim_exclusivo"));
            assertEquals("C01-D01", p.getValue("departamento"));
            assertEquals(null, p.getValue("colaborador"));
            assertEquals(List.of("success"), p.getValue("status_cobrados"));
            assertEquals(List.of("Pipeline Interno", "API Direta"), p.getValue("aplicacoes_agente"));
        }
        assertTrue(sql.getAllValues().get(Agrupamento.DEPARTAMENTO.ordinal()).contains("\"department_id\" as id"));
        assertTrue(sql.getAllValues().get(Agrupamento.COLABORADOR.ordinal()).contains("\"employee_id\" as id"));
        assertTrue(sql.getAllValues().get(Agrupamento.FERRAMENTA.ordinal()).contains("\"ai_tool_id\" as id"));
    }

    @Test
    void listasVaziasNaoGeramSqlInvalido() {
        FiltroCusto vazio = new FiltroCusto("C01", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, "C01-E0001", List.of(), List.of());
        consultas.uso(vazio, Agrupamento.TOTAL);

        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForList(anyString(), params.capture());
        assertEquals(List.of(""), params.getValue().getValue("status_cobrados"));
        assertEquals(List.of(""), params.getValue().getValue("aplicacoes_agente"));
        assertEquals("C01-E0001", params.getValue().getValue("colaborador"));
    }

    @Test
    void cadastrosEPrecosDevemSairComNomesFisicos() {
        assertEquals(1, consultas.empresas().size());
        assertEquals(1, consultas.departamentos("C01").size());
        assertEquals(1, consultas.colaboradores("C01").size());
        assertEquals(1, consultas.ferramentas().size());
        assertEquals(1, consultas.precos().size());
        assertEquals("2025-06-01", consultas.periodo().get("inicio"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc, atLeastOnce()).queryForList(sql.capture(), params.capture());
        verify(jdbc).queryForMap(sql.capture(), params.capture());
        for (int i = 0; i < sql.getAllValues().size(); i++) {
            conferirSql(sql.getAllValues().get(i), (MapSqlParameterSource) params.getAllValues().get(i));
        }
        String todos = String.join("\n", sql.getAllValues());
        assertTrue(todos.contains("\"public\".\"ai_models\""));
        assertTrue(todos.contains("\"input_price_usd_per_1k_tokens\""));
        assertTrue(todos.contains("\"output_price_usd_per_1k_tokens\""));
        assertTrue(todos.contains("\"public\".\"companies\""));
        assertTrue(todos.contains("\"public\".\"departments\""));
        assertTrue(todos.contains("\"public\".\"employees\""));
        assertTrue(todos.contains("\"public\".\"ai_tools\""));
    }

    @Test
    void pertencimentoAEmpresaDeveConsultarOBanco() {
        assertTrue(consultas.empresaExiste("C01"));
        assertTrue(consultas.departamentoDaEmpresa("C01", "C01-D01"));
        assertTrue(consultas.colaboradorDaEmpresa("C01", "C01-E0001"));

        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Boolean.class))).thenReturn(null);
        assertFalse(consultas.colaboradorDaEmpresa("C01", "C02-E0001"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc, atLeastOnce()).queryForObject(sql.capture(), params.capture(), eq(Boolean.class));
        for (int i = 0; i < sql.getAllValues().size(); i++) {
            conferirSql(sql.getAllValues().get(i), (MapSqlParameterSource) params.getAllValues().get(i));
        }
        assertEquals("C02-E0001", params.getValue().getValue("colaborador"));
    }
}
