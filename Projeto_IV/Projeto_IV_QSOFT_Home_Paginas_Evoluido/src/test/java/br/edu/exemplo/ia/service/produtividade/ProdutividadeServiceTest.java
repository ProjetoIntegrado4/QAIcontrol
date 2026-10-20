package br.edu.exemplo.ia.service.produtividade;

import br.edu.exemplo.ia.config.ProdutividadeTestes;
import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import br.edu.exemplo.ia.repository.produtividade.ConsultaPainel;
import br.edu.exemplo.ia.repository.produtividade.EsquemaBase;
import br.edu.exemplo.ia.repository.produtividade.ResumoConsultas;
import br.edu.exemplo.ia.service.produtividade.secao.Disponibilidade;
import br.edu.exemplo.ia.service.produtividade.secao.SecaoPainel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdutividadeServiceTest {

    private ResumoConsultas resumo;
    private ConsultaPainel db;
    private CatalogoBanco catalogo;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        resumo = mock(ResumoConsultas.class);
        db = mock(ConsultaPainel.class);
        catalogo = mock(CatalogoBanco.class);
        executor = Executors.newFixedThreadPool(2);
        when(db.consultar(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        when(catalogo.faltantes(anyCollection())).thenReturn(List.of());
        when(resumo.empresas()).thenReturn(List.of(mapa("id", "C01", "nome", "Alpha", "setor", "Varejo")));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ProdutividadeService service(SecaoPainel... secoes) {
        return new ProdutividadeService(List.of(secoes), resumo, db, catalogo,
                new EsquemaBase(ProdutividadeTestes.propriedades()), executor);
    }

    private static SecaoPainel secao(String chave, Set<String> requisitos, BiFunction<String, Disponibilidade, Object> calculo) {
        return new SecaoPainel() {
            public String chave() { return chave; }
            public Set<String> requisitos() { return requisitos; }
            public Object calcular(String empresa, Disponibilidade d) { return calculo.apply(empresa, d); }
        };
    }

    @Test
    void resumoDeveIncluirTodasAsEmpresasPeriodoQualidadeEMapeamento() {
        when(resumo.periodo()).thenReturn(mapa("inicio", "2025-01", "fim", "2025-12", "linhas_uso", 100L));
        when(resumo.usoSemCentroDeCusto()).thenReturn(3L);
        when(resumo.pullRequestsInconsistentes()).thenReturn(mapa("lead_negativo", 1L));
        when(resumo.funcionariosSemCentroDeCusto()).thenReturn(2L);

        Map<String, Object> r = service().resumo();

        verify(catalogo).atualizar();
        List<?> empresas = (List<?>) r.get("empresas");
        assertEquals(2, empresas.size());
        assertEquals("ALL", ((Map<?, ?>) empresas.get(0)).get("id"));
        assertEquals("1 empresas", ((Map<?, ?>) empresas.get(0)).get("setor"));
        assertEquals(List.of("2025-01", "2025-12"), r.get("periodo"));
        assertEquals(mapa("linhas_uso", 100L, "uso_sem_cc", 3L, "lead_negativo", 1L, "sem_cc", 2L), r.get("qualidade"));
        assertEquals(90, r.get("janela_dias"));
        assertEquals("ai_usage_logs", ((Map<?, ?>) r.get("tabelas")).get("uso"));
        assertEquals(Map.of(), r.get("indisponiveis"));
        assertTrue(r.containsKey("atualizado_em"));
    }

    @Test
    void resumoSemTabelaDeUsoDeveTerPeriodoVazioEAviso() {
        when(catalogo.faltantes(anyCollection())).thenAnswer(i -> {
            Collection<String> reqs = i.getArgument(0);
            return reqs.stream().anyMatch(x -> x.startsWith("uso.")) ? List.of("ai_usage_logs") : List.of();
        });
        when(resumo.pullRequestsInconsistentes()).thenReturn(Map.of());

        Map<String, Object> r = service().resumo();

        assertEquals(List.of(), r.get("periodo"));
        assertEquals(Set.of("periodo", "qualidade.uso"), ((Map<?, ?>) r.get("indisponiveis")).keySet());
        verify(resumo, never()).periodo();
    }

    @Test
    void resumoComBaseSemDadosDeUsoDeveTerPeriodoVazio() {
        when(resumo.periodo()).thenReturn(mapa("inicio", null, "fim", null, "linhas_uso", 0L));
        when(resumo.pullRequestsInconsistentes()).thenReturn(Map.of());
        assertEquals(List.of(), service().resumo().get("periodo"));
    }

    @Test
    void resumoSemCadastroDeEmpresasDeveFalhar() {
        when(catalogo.faltantes(anyCollection())).thenReturn(List.of("companies"));
        assertThrows(IllegalStateException.class, () -> service().resumo());
    }

    @Test
    void painelDeEmpresaInexistenteDeveFalhar() {
        assertThrows(NoSuchElementException.class, () -> service().painel("X99"));
    }

    @Test
    void painelDeveRodarAsSecoesETirarAsQueNaoTemDados() {
        when(catalogo.faltantes(anyCollection())).thenAnswer(i ->
                ((Collection<?>) i.getArgument(0)).contains("eventos_seguranca.tipo") ? List.of("security_events") : List.of());
        SecaoPainel a = secao("a", Set.of("uso.data"), (e, d) -> "A-" + e);
        SecaoPainel b = secao("b", Set.of("eventos_seguranca.tipo"), (e, d) -> "nunca");
        SecaoPainel c = secao("c", Set.of(), (e, d) -> d.tem("c.parte", Set.of("eventos_seguranca.tipo")) ? 1 : 0);

        Map<String, Object> p = service(a, b, c).painel("C01");

        assertEquals(List.of("a", "b", "c", "indisponiveis"), List.copyOf(p.keySet()));
        assertEquals("A-C01", p.get("a"));
        assertNull(p.get("b"));
        assertEquals(0, p.get("c"));
        assertEquals(Map.of("b", "faltam no banco: security_events", "c.parte", "faltam no banco: security_events"),
                p.get("indisponiveis"));
    }

    @Test
    void erroNumaSecaoDevePropagarAExcecaoOriginal() {
        IllegalStateException erro = new IllegalStateException("banco fora do ar");
        SecaoPainel falha = secao("x", Set.of(), (e, d) -> { throw erro; });

        assertSame(erro, assertThrows(IllegalStateException.class, () -> service(falha).painel("ALL")));
    }
}
