package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.repository.ProdutividadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdutividadeServiceTest {

    private ProdutividadeRepository repo;
    private ProdutividadeService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProdutividadeRepository.class);
        when(repo.consultar(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        when(repo.empresas()).thenReturn(List.of(mapa("id", "C01", "nome", "Alpha", "setor", "Varejo")));
        service = new ProdutividadeService(repo);
    }

    @Test
    void resumoDeveIncluirOpcaoTodasAsEmpresasEPeriodo() {
        when(repo.periodo()).thenReturn(mapa("inicio", "2025-01", "fim", "2025-12", "linhas_uso", 100L, "uso_sem_cc", 3L));
        when(repo.qualidade()).thenReturn(mapa("lead_negativo", 1L));

        Map<String, Object> r = service.resumo();

        List<?> empresas = (List<?>) r.get("empresas");
        assertEquals(2, empresas.size());
        assertEquals("ALL", ((Map<?, ?>) empresas.get(0)).get("id"));
        assertEquals("1 empresas", ((Map<?, ?>) empresas.get(0)).get("setor"));
        assertEquals(List.of("2025-01", "2025-12"), r.get("periodo"));
        Map<?, ?> q = (Map<?, ?>) r.get("qualidade");
        assertEquals(3L, q.get("uso_sem_cc"));
        assertEquals(100L, q.get("linhas_uso"));
        assertTrue(r.containsKey("atualizado_em"));
    }

    @Test
    void resumoSemDadosDeUsoDeveTerPeriodoVazio() {
        when(repo.periodo()).thenReturn(mapa("inicio", null, "fim", null, "linhas_uso", 0L, "uso_sem_cc", 0L));
        when(repo.qualidade()).thenReturn(Map.of());

        assertEquals(List.of(), service.resumo().get("periodo"));
    }

    @Test
    void painelDeEmpresaInexistenteDeveFalhar() {
        assertThrows(NoSuchElementException.class, () -> service.painel("X99"));
    }

    @Test
    void painelDeveCalcularIndicadoresDaEmpresa() {
        prepararDados();

        Map<String, Object> p = service.painel("C01");

        Map<?, ?> kpis = (Map<?, ?>) p.get("kpis");
        assertEquals(10L, kpis.get("funcionarios"));
        assertEquals(80.0, kpis.get("adocao"));
        assertEquals(90.0, kpis.get("sucesso"));
        assertEquals(1234.57, kpis.get("custo"));
        assertEquals(10.0, kpis.get("fora_politica"));
        assertEquals(75.0, kpis.get("trein_concl"));
        assertNull(kpis.get("latencia"));

        Map<?, ?> mes = (Map<?, ?>) ((List<?>) p.get("mensal")).get(0);
        assertEquals("2025-01", mes.get("mes"));
        assertEquals(95.5, mes.get("sucesso"));

        Map<?, ?> area = (Map<?, ?>) ((List<?>) p.get("areas")).get(0);
        assertEquals(50.0, area.get("adocao"));
        Map<?, ?> areaVazia = (Map<?, ?>) ((List<?>) p.get("areas")).get(1);
        assertEquals(0.0, areaVazia.get("adocao"));

        Map<?, ?> ferramenta = (Map<?, ?>) ((List<?>) p.get("ferramentas")).get(0);
        assertEquals(2L, ferramenta.get("lic_ociosas"));
        Map<?, ?> top = (Map<?, ?>) ((List<?>) p.get("top")).get(0);
        assertEquals(300L, top.get("req"));

        Map<?, ?> gov = (Map<?, ?>) p.get("gov");
        assertEquals(Map.of("Vazamento", 4L), gov.get("eventos_tipo"));
        assertEquals(Map.of("Alta", 2L), gov.get("eventos_sev"));
        assertEquals(Map.of("Concluído", 3L), gov.get("trein_status"));

        Map<?, ?> eng = (Map<?, ?>) p.get("eng");
        assertEquals(0.42, eng.get("corr"));
        Map<?, ?> coorte = (Map<?, ?>) ((List<?>) eng.get("coortes")).get(0);
        assertEquals("Uso alto", coorte.get("faixa"));
        assertEquals(20L, ((Map<?, ?>) eng.get("totais")).get("prs"));
        assertEquals(1, ((List<?>) eng.get("mensal")).size());
    }

    @Test
    void painelConsolidadoSemEngenhariaNemDivisoresDeveUsarNulos() {
        when(repo.kpis("ALL")).thenReturn(mapa("funcionarios", 0L, "requisicoes", 0L, "requisicoes_total", 0L,
                "trein_total", 0L, "latencia", Double.NaN));
        when(repo.engTotais("ALL")).thenReturn(mapa("prs", 0L));

        Map<String, Object> p = service.painel("ALL");

        Map<?, ?> kpis = (Map<?, ?>) p.get("kpis");
        assertNull(kpis.get("sucesso"));
        assertNull(kpis.get("trein_concl"));
        assertNull(kpis.get("latencia"));
        assertEquals(0.0, kpis.get("adocao"));
        assertNull(p.get("eng"));
        assertEquals(List.of(), p.get("mensal"));
    }

    @Test
    void erroNumaSecaoDevePropagarAExcecaoOriginal() {
        IllegalStateException erro = new IllegalStateException("banco fora do ar");
        when(repo.kpis(anyString())).thenThrow(erro);
        when(repo.engTotais(anyString())).thenReturn(mapa("prs", 0L));

        assertSame(erro, assertThrows(IllegalStateException.class, () -> service.painel("ALL")));
    }

    private void prepararDados() {
        when(repo.kpis("C01")).thenReturn(mapa("funcionarios", 10L, "usuarios", 9L, "usuarios_ativos", 8L,
                "requisicoes", 90L, "requisicoes_total", 100L, "tokens", 5000L, "latencia", null,
                "custo", new BigDecimal("1234.5678"), "orcamento", 2000, "lic_ativas", 5L, "lic_ociosas", 1L,
                "req_fora", 9L, "trein_total", 4L, "trein_concluidos", 3L, "trein_pessoas", 2L,
                "eventos", 4L, "eventos_graves", 2L));
        when(repo.mensal("C01")).thenReturn(List.of(mapa("mes", "2025-01", "req", 90L, "ativos", 8L,
                "sucesso", 95.49, "custo", 10.111, "orcamento", 20.0)));
        when(repo.areas("C01")).thenReturn(List.of(
                mapa("area", "TI", "funcionarios", 4L, "usuarios", 2L, "req", 50L, "req_usuario", 25.0,
                        "custo", 5.0, "trein", 1.0),
                mapa("area", "RH", "funcionarios", 0L, "usuarios", 0L, "req", 0L, "req_usuario", null,
                        "custo", null, "trein", null)));
        when(repo.ferramentas("C01")).thenReturn(List.of(mapa("id", "T1", "nome", "ChatGPT", "categoria", "LLM",
                "req", 80L, "usuarios", 7L, "custo", 3.5, "lic_ativas", 5L, "lic_ociosas", 2L, "fora", 1.25)));
        when(repo.top("C01")).thenReturn(List.of(mapa("id", "E1", "cargo", "Dev", "senioridade", "Sênior",
                "area", "TI", "req", 300L, "ferramentas", 2L, "dias", 40L)));
        when(repo.eventosPorTipo("C01")).thenReturn(List.of(mapa("chave", "Vazamento", "total", 4L)));
        when(repo.eventosPorSeveridade("C01")).thenReturn(List.of(mapa("chave", "Alta", "total", 2L)));
        when(repo.treinamentosPorSituacao("C01")).thenReturn(List.of(mapa("chave", "Concluído", "total", 3L)));
        when(repo.engTotais("C01")).thenReturn(mapa("prs", 20L, "merged", 15L, "lead", 30.4, "build_ok", 90.0,
                "deploy_falha", 5.0, "devs", 4L));
        when(repo.engMensal("C01")).thenReturn(List.of(mapa("mes", "2025-01", "prs", 20L, "merged", 15L,
                "lead", 30.4, "build_ok", 90.0, "deploy_falha", 5.0)));
        when(repo.engCoortes("C01")).thenReturn(List.of(mapa("faixa", 3, "devs", 2L, "ai_mediana", 100L,
                "prs", 6.0, "merged", 5.0, "lead", 20.0)));
        when(repo.engCorrelacao("C01")).thenReturn(0.4201);
    }

    private static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
