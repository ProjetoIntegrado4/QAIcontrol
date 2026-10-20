package br.edu.exemplo.ia.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutividadeConfigTest {

    @Test
    void yamlDeveTrazerMapeamentoEValores() {
        ProdutividadeProperties p = ProdutividadeTestes.propriedades();

        assertEquals("public", p.schema());
        assertEquals(90, p.janelaRecenteDias());
        assertEquals("ai_usage_logs", p.tabelas().get("uso").nome());
        assertEquals("request_status", p.tabelas().get("uso").colunas().get("status"));
        assertEquals(List.of("success"), p.valores().get("uso-sucesso"));
    }

    @Test
    void mapasNulosViramVazios() {
        ProdutividadeProperties p = new ProdutividadeProperties("public", "x", 90, 12, "", null, null);
        assertEquals(Map.of(), p.tabelas());
        assertEquals(Map.of(), p.valores());
        assertEquals(Map.of(), new ProdutividadeProperties.Tabela("t", null).colunas());
    }

    @Test
    void executorDeveRodarTarefas() throws Exception {
        ExecutorService executor = new ProdutividadeConfig().produtividadeExecutor();
        assertEquals(2, executor.submit(() -> 1 + 1).get());
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }
}
