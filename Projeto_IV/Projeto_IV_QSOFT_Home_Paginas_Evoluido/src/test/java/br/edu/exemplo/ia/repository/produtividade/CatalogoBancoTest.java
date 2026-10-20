package br.edu.exemplo.ia.repository.produtividade;

import br.edu.exemplo.ia.config.ProdutividadeTestes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogoBancoTest {

    private JdbcTemplate jdbc;
    private CatalogoBanco catalogo;

    @BeforeEach
    void setUp() throws Exception {
        jdbc = mock(JdbcTemplate.class);
        String[][] colunas = {{"ai_usage_logs", "employee_id"}, {"ai_usage_logs", "request_status"}, {"companies", "company_id"}};
        doAnswer(i -> {
            RowCallbackHandler h = i.getArgument(1);
            for (String[] c : colunas) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getString(1)).thenReturn(c[0]);
                when(rs.getString(2)).thenReturn(c[1]);
                h.processRow(rs);
            }
            return null;
        }).when(jdbc).query(anyString(), any(RowCallbackHandler.class), eq("public"));
        catalogo = new CatalogoBanco(jdbc, new EsquemaBase(ProdutividadeTestes.propriedades()));
    }

    @Test
    void deveApontarTabelasEColunasAusentesComNomesFisicos() {
        assertEquals(List.of(), catalogo.faltantes(List.of("uso", "uso.funcionario", "uso.status")));
        assertEquals(List.of("ai_usage_logs.latency_ms", "ai_billing", "x.y (não mapeado em produtividade.yml)"),
                catalogo.faltantes(List.of("uso.latencia", "faturamento", "faturamento.custo", "x.y")));
        assertTrue(catalogo.possui("empresas.id"));
        assertFalse(catalogo.possui("empresas.nome"));
    }

    @Test
    void deveLerOBancoUmaVezAteSerAtualizado() {
        catalogo.possui("uso");
        catalogo.possui("empresas");
        verify(jdbc, times(1)).query(anyString(), any(RowCallbackHandler.class), eq("public"));

        catalogo.atualizar();
        catalogo.possui("uso");
        verify(jdbc, times(2)).query(anyString(), any(RowCallbackHandler.class), eq("public"));
    }
}
