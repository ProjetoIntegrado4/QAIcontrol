package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.AreasConsultas;
import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import br.edu.exemplo.ia.repository.produtividade.ColaboradoresConsultas;
import br.edu.exemplo.ia.repository.produtividade.EngenhariaConsultas;
import br.edu.exemplo.ia.repository.produtividade.FerramentasConsultas;
import br.edu.exemplo.ia.repository.produtividade.GovernancaConsultas;
import br.edu.exemplo.ia.repository.produtividade.IndicadoresConsultas;
import br.edu.exemplo.ia.repository.produtividade.MensalConsultas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecoesTest {

    private CatalogoBanco catalogo;
    private Disponibilidade disp;

    @BeforeEach
    void setUp() {
        catalogo = mock(CatalogoBanco.class);
        when(catalogo.faltantes(anyCollection())).thenReturn(List.of());
        disp = new Disponibilidade(catalogo);
    }

    /** Simula um banco sem a tabela lógica indicada (ex.: "eventos_seguranca"). */
    private void semTabela(String logica, String fisica) {
        when(catalogo.faltantes(anyCollection())).thenAnswer(i -> {
            Collection<String> reqs = i.getArgument(0);
            return reqs.stream().anyMatch(r -> r.equals(logica) || r.startsWith(logica + ".")) ? List.of(fisica) : List.of();
        });
    }

    @Test
    void indicadoresDevemCalcularPercentuaisEArredondar() {
        IndicadoresConsultas repo = mock(IndicadoresConsultas.class);
        when(repo.uso("C01")).thenReturn(mapa("funcionarios", 10L, "usuarios", 9L, "usuarios_ativos", 8L,
                "requisicoes", 90L, "requisicoes_total", 100L, "tokens", 5000L, "latencia", null));
        when(repo.politica("C01")).thenReturn(mapa("req_fora", 9L));
        when(repo.custo("C01")).thenReturn(mapa("custo", new BigDecimal("1234.5678")));
        when(repo.orcamento("C01")).thenReturn(mapa("orcamento", 2000));
        when(repo.licencas("C01")).thenReturn(mapa("lic_ativas", 5L, "lic_ociosas", 1L));
        when(repo.treinamento("C01")).thenReturn(mapa("trein_total", 4L, "trein_concluidos", 3L, "trein_pessoas", 2L));
        when(repo.seguranca("C01")).thenReturn(mapa("eventos", 4L, "eventos_graves", 2L));

        IndicadoresSecao s = new IndicadoresSecao(repo);
        Map<?, ?> k = (Map<?, ?>) s.calcular("C01", disp);

        assertEquals("kpis", s.chave());
        assertTrue(usa(s, "uso"));
        assertEquals(10L, k.get("funcionarios"));
        assertEquals(80.0, k.get("adocao"));
        assertEquals(90.0, k.get("sucesso"));
        assertEquals(1234.57, k.get("custo"));
        assertEquals(2000.0, k.get("orcamento"));
        assertEquals(10.0, k.get("fora_politica"));
        assertEquals(75.0, k.get("trein_concl"));
        assertEquals(1L, k.get("lic_ociosas"));
        assertEquals(2L, k.get("eventos_graves"));
        assertNull(k.get("latencia"));
    }

    @Test
    void indicadoresSemSegurancaNemDivisoresDevemVirNulos() {
        semTabela("eventos_seguranca", "security_events");
        IndicadoresConsultas repo = mock(IndicadoresConsultas.class);
        when(repo.uso("ALL")).thenReturn(mapa("funcionarios", 0L, "requisicoes", 0L, "requisicoes_total", 0L,
                "latencia", Double.NaN));
        when(repo.politica("ALL")).thenReturn(Map.of());
        when(repo.custo("ALL")).thenReturn(Map.of());
        when(repo.orcamento("ALL")).thenReturn(Map.of());
        when(repo.licencas("ALL")).thenReturn(Map.of());
        when(repo.treinamento("ALL")).thenReturn(mapa("trein_total", 0L));

        Map<?, ?> k = (Map<?, ?>) new IndicadoresSecao(repo).calcular("ALL", disp);

        assertNull(k.get("sucesso"));
        assertNull(k.get("trein_concl"));
        assertNull(k.get("latencia"));
        assertNull(k.get("eventos"));
        assertNull(k.get("fora_politica"));
        assertEquals(0.0, k.get("adocao"));
        verify(repo, never()).seguranca("ALL");
        assertEquals(Map.of("kpis.seguranca", "faltam no banco: security_events"), disp.faltas());
    }

    @Test
    void mensalDeveJuntarCustoEOrcamentoPorMes() {
        MensalConsultas repo = mock(MensalConsultas.class);
        when(repo.uso("C01")).thenReturn(List.of(mapa("mes", "2025-01", "req", 90L, "ativos", 8L, "sucesso", 95.49),
                mapa("mes", "2025-02", "req", 10L, "ativos", 1L, "sucesso", 100.0)));
        when(repo.custo("C01")).thenReturn(List.of(mapa("mes", "2025-01", "custo", 10.111)));
        when(repo.orcamento("C01")).thenReturn(List.of(mapa("mes", "2025-01", "orcamento", 20.0)));

        MensalSecao s = new MensalSecao(repo);
        List<?> meses = (List<?>) s.calcular("C01", disp);

        assertEquals("mensal", s.chave());
        assertTrue(usa(s, "uso"));
        Map<?, ?> jan = (Map<?, ?>) meses.get(0);
        assertEquals(95.5, jan.get("sucesso"));
        assertEquals(10.11, jan.get("custo"));
        assertEquals(20.0, jan.get("orcamento"));
        assertEquals(0.0, ((Map<?, ?>) meses.get(1)).get("custo"));
    }

    @Test
    void mensalSemFaturamentoDeveTerCustoNulo() {
        semTabela("faturamento", "ai_billing");
        MensalConsultas repo = mock(MensalConsultas.class);
        when(repo.uso("C01")).thenReturn(List.of(mapa("mes", "2025-01", "req", 1L)));
        when(repo.orcamento("C01")).thenReturn(List.of());

        Map<?, ?> jan = (Map<?, ?>) ((List<?>) new MensalSecao(repo).calcular("C01", disp)).get(0);

        assertNull(jan.get("custo"));
        assertEquals(0.0, jan.get("orcamento"));
    }

    @Test
    void areasDevemCalcularAdocaoComDivisorZero() {
        AreasConsultas repo = mock(AreasConsultas.class);
        when(repo.uso("C01")).thenReturn(List.of(
                mapa("area", "TI", "funcionarios", 4L, "usuarios", 2L, "req", 50L, "req_usuario", 25.0),
                mapa("area", "RH", "funcionarios", 0L, "usuarios", 0L, "req", 0L, "req_usuario", null)));
        when(repo.custo("C01")).thenReturn(List.of(mapa("area", "TI", "custo", 5.0)));
        when(repo.treinamento("C01")).thenReturn(List.of(mapa("area", "TI", "trein", 33.333)));

        AreasSecao s = new AreasSecao(repo);
        List<?> areas = (List<?>) s.calcular("C01", disp);

        assertEquals("areas", s.chave());
        assertTrue(usa(s, "departamentos"));
        Map<?, ?> ti = (Map<?, ?>) areas.get(0);
        assertEquals(50.0, ti.get("adocao"));
        assertEquals(33.3, ti.get("trein"));
        Map<?, ?> rh = (Map<?, ?>) areas.get(1);
        assertEquals(0.0, rh.get("adocao"));
        assertEquals(0.0, rh.get("custo"));
        assertNull(rh.get("trein"));
    }

    @Test
    void ferramentasDevemJuntarCadastroCustoLicencasEPolitica() {
        FerramentasConsultas repo = mock(FerramentasConsultas.class);
        when(repo.uso("C01")).thenReturn(List.of(mapa("id", "T1", "req", 80L, "usuarios", 7L),
                mapa("id", "T9", "req", 0L, "usuarios", 0L)));
        when(repo.cadastro()).thenReturn(List.of(mapa("id", "T1", "nome", "ChatGPT", "categoria", "LLM")));
        when(repo.custo("C01")).thenReturn(List.of(mapa("id", "T1", "custo", 3.5)));
        when(repo.licencas("C01")).thenReturn(List.of(mapa("id", "T1", "lic_ativas", 5L, "lic_ociosas", 2L)));
        when(repo.foraDaPolitica("C01")).thenReturn(List.of(mapa("id", "T1", "req_fora", 1L)));

        FerramentasSecao s = new FerramentasSecao(repo);
        List<?> f = (List<?>) s.calcular("C01", disp);

        assertEquals("ferramentas", s.chave());
        assertTrue(usa(s, "uso"));
        Map<?, ?> t1 = (Map<?, ?>) f.get(0);
        assertEquals("ChatGPT", t1.get("nome"));
        assertEquals(2L, t1.get("lic_ociosas"));
        assertEquals(1.2, t1.get("fora"));
        Map<?, ?> t9 = (Map<?, ?>) f.get(1);
        assertEquals("T9", t9.get("nome"));
        assertNull(t9.get("fora"));
    }

    @Test
    void ferramentasSemCadastroNemLicencasUsamOId() {
        when(catalogo.faltantes(anyCollection())).thenAnswer(i -> {
            Collection<String> reqs = i.getArgument(0);
            return reqs.stream().anyMatch(r -> r.startsWith("ferramentas.") || r.startsWith("licencas.")) ? List.of("x") : List.of();
        });
        FerramentasConsultas repo = mock(FerramentasConsultas.class);
        when(repo.uso("C01")).thenReturn(List.of(mapa("id", "T1", "req", 1L)));
        when(repo.custo("C01")).thenReturn(List.of());
        when(repo.foraDaPolitica("C01")).thenReturn(List.of());

        Map<?, ?> t1 = (Map<?, ?>) ((List<?>) new FerramentasSecao(repo).calcular("C01", disp)).get(0);

        assertEquals("T1", t1.get("nome"));
        assertNull(t1.get("lic_ativas"));
        assertTrue(disp.faltas().containsKey("ferramentas.cadastro"));
    }

    @Test
    void engenhariaDeveMontarTotaisCoortesECorrelacao() {
        EngenhariaConsultas repo = mock(EngenhariaConsultas.class);
        when(repo.totaisPullRequests("C01")).thenReturn(mapa("prs", 20L, "merged", 15L, "lead", 30.4, "devs", 4L));
        when(repo.totaisBuilds("C01")).thenReturn(mapa("build_ok", 90.0));
        when(repo.totaisDeploys("C01")).thenReturn(mapa("deploy_falha", 5.0));
        when(repo.pullRequestsPorMes("C01")).thenReturn(List.of(mapa("mes", "2025-01", "prs", 20L, "merged", 15L, "lead", 30.4)));
        when(repo.buildsPorMes("C01")).thenReturn(List.of(mapa("mes", "2025-01", "build_ok", 90.0)));
        when(repo.deploysPorMes("C01")).thenReturn(List.of());
        when(repo.coortes("C01")).thenReturn(List.of(mapa("faixa", 3, "devs", 2L, "ai_mediana", 100L,
                "prs", 6.0, "merged", 5.0, "lead", 20.0)));
        when(repo.correlacao("C01")).thenReturn(0.4201);
        when(repo.empresasComProjetos()).thenReturn(List.of("C01"));

        EngenhariaSecao s = new EngenhariaSecao(repo);
        Map<?, ?> eng = (Map<?, ?>) s.calcular("C01", disp);

        assertEquals("eng", s.chave());
        assertTrue(usa(s, "pull_requests"));
        assertEquals(0.42, eng.get("corr"));
        assertEquals("Uso alto", ((Map<?, ?>) ((List<?>) eng.get("coortes")).get(0)).get("faixa"));
        Map<?, ?> totais = (Map<?, ?>) eng.get("totais");
        assertEquals(20L, totais.get("prs"));
        assertEquals(5.0, totais.get("deploy_falha"));
        Map<?, ?> mes = (Map<?, ?>) ((List<?>) eng.get("mensal")).get(0);
        assertEquals(90.0, mes.get("build_ok"));
        assertNull(mes.get("deploy_falha"));
        assertEquals(List.of("C01"), eng.get("empresas_com_projetos"));
    }

    @Test
    void engenhariaSemPullRequestsDeveSerNula() {
        EngenhariaConsultas repo = mock(EngenhariaConsultas.class);
        when(repo.totaisPullRequests("ALL")).thenReturn(mapa("prs", 0L));
        assertNull(new EngenhariaSecao(repo).calcular("ALL", disp));
    }

    @Test
    void governancaDeveContarPorChaveEAceitarFaltaDeSeguranca() {
        GovernancaConsultas repo = mock(GovernancaConsultas.class);
        when(repo.eventosPorTipo("C01")).thenReturn(List.of(mapa("chave", "Vazamento", "total", 4L)));
        when(repo.eventosPorSeveridade("C01")).thenReturn(List.of(mapa("chave", "Alta", "total", 2L)));
        when(repo.treinamentosPorSituacao("C01")).thenReturn(List.of(mapa("chave", "Concluido", "total", 3L)));

        GovernancaSecao s = new GovernancaSecao(repo);
        Map<?, ?> gov = (Map<?, ?>) s.calcular("C01", disp);

        assertEquals("gov", s.chave());
        assertEquals(Set.of(), s.requisitos());
        assertEquals(Map.of("Vazamento", 4L), gov.get("eventos_tipo"));
        assertEquals(Map.of("Alta", 2L), gov.get("eventos_sev"));
        assertEquals(Map.of("Concluido", 3L), gov.get("trein_status"));

        semTabela("eventos_seguranca", "security_events");
        gov = (Map<?, ?>) s.calcular("C01", disp);
        assertNull(gov.get("eventos_tipo"));
        assertEquals(Map.of("Concluido", 3L), gov.get("trein_status"));
    }

    @Test
    void colaboradoresDevemConverterNumeros() {
        ColaboradoresConsultas repo = mock(ColaboradoresConsultas.class);
        when(repo.maisAtivos("C01")).thenReturn(List.of(mapa("id", "E1", "cargo", "Dev", "senioridade", "Sênior",
                "area", "TI", "req", 300, "ferramentas", 2L, "dias", null)));

        ColaboradoresSecao s = new ColaboradoresSecao(repo);
        Map<?, ?> top = (Map<?, ?>) ((List<?>) s.calcular("C01", disp)).get(0);

        assertEquals("top", s.chave());
        assertTrue(usa(s, "funcionarios"));
        assertEquals(300L, top.get("req"));
        assertEquals(0L, top.get("dias"));
    }

    @Test
    void requisitosDeDevemUnirConjuntos() {
        assertEquals(Set.of("a", "b"), Requisitos.de(Set.of("a"), Set.of("a", "b")));
    }

    private static boolean usa(SecaoPainel s, String tabela) {
        return s.requisitos().stream().anyMatch(r -> r.startsWith(tabela + "."));
    }

    static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
