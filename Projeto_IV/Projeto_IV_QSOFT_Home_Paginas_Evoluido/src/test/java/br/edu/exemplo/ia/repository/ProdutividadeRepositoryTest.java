package br.edu.exemplo.ia.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdutividadeRepositoryTest {

    private NamedParameterJdbcTemplate jdbc;
    private ProdutividadeRepository repo;
    private final List<Map<String, Object>> linhas = List.of(Map.of("id", "C01"));
    private final Map<String, Object> linha = Map.of("total", 1L);

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(i -> ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null));
        when(jdbc.getJdbcTemplate()).thenReturn(mock(JdbcTemplate.class));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(linhas);
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class))).thenReturn(linhas);
        when(jdbc.queryForMap(anyString(), anyMap())).thenReturn(linha);
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(linha);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Double.class))).thenReturn(0.5);
        repo = new ProdutividadeRepository(jdbc, tx);
    }

    @Test
    void consultarDeveDesligarJitDentroDaTransacao() {
        assertEquals("ok", repo.consultar(() -> "ok"));
        verify(jdbc.getJdbcTemplate()).execute("set local jit = off");
    }

    @Test
    void consultasGeraisNaoDependemDaEmpresa() {
        assertEquals(linhas, repo.empresas());
        assertEquals(linha, repo.periodo());
        assertEquals(linha, repo.qualidade());
        verify(jdbc).queryForList(contains("from companies"), anyMap());
    }

    @Test
    void consultasDoPainelDevemFiltrarPelaEmpresa() {
        String emp = "C01";
        assertEquals(linha, repo.kpis(emp));
        assertEquals(linhas, repo.mensal(emp));
        assertEquals(linhas, repo.areas(emp));
        assertEquals(linhas, repo.ferramentas(emp));
        assertEquals(linhas, repo.top(emp));
        assertEquals(linhas, repo.eventosPorTipo(emp));
        assertEquals(linhas, repo.eventosPorSeveridade(emp));
        assertEquals(linhas, repo.treinamentosPorSituacao(emp));
        assertEquals(linhas, repo.engMensal(emp));
        assertEquals(linha, repo.engTotais(emp));
        assertEquals(linhas, repo.engCoortes(emp));
        assertEquals(0.5, repo.engCorrelacao(emp));

        verify(jdbc, atLeast(8)).queryForList(anyString(), argThat((SqlParameterSource p) ->
                p instanceof MapSqlParameterSource m && emp.equals(m.getValue("emp"))));
    }
}
