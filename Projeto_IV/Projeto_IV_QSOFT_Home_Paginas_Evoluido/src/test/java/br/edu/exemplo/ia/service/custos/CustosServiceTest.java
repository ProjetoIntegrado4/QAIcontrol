package br.edu.exemplo.ia.service.custos;

import br.edu.exemplo.ia.config.CustosProperties;
import br.edu.exemplo.ia.dto.CustoItemResponse;
import br.edu.exemplo.ia.dto.CustoOpcoesResponse;
import br.edu.exemplo.ia.dto.CustoRequest;
import br.edu.exemplo.ia.dto.CustoResponse;
import br.edu.exemplo.ia.repository.custos.Agrupamento;
import br.edu.exemplo.ia.repository.custos.CustosConsultas;
import br.edu.exemplo.ia.repository.custos.FiltroCusto;
import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import br.edu.exemplo.ia.repository.produtividade.ConsultaPainel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes da US PP-18 (Calcular Custos de IA, incluindo Agentes) – Tasks PS-60, PS-62 e PS-338.
 * Os dois primeiros testes são os cenários do BDD; os demais cobrem filtros, regra e validações.
 *
 * <p>Preços usados (USD por 1k tokens): M01 = 0,005 entrada / 0,015 saída; M03 = 0,015 / 0,075; M99 sem preço.
 * Uso direto: 1000 entrada + 2000 saída em M01 = 0,035. Agente: 2000 entrada + 1000 saída em M03 = 0,105.
 */
class CustosServiceTest {

    private static final String SEM_USO = "Nenhum uso de IA registrado no período selecionado";

    private CustosConsultas repo;
    private CatalogoBanco catalogo;
    private CustosService service;

    @BeforeEach
    void prepararCenario() {
        repo = mock(CustosConsultas.class);
        catalogo = mock(CatalogoBanco.class);
        ConsultaPainel db = mock(ConsultaPainel.class);
        when(db.consultar(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        when(catalogo.faltantes(anyCollection())).thenReturn(List.of());
        service = new CustosService(repo, db, catalogo, new RegraCusto(RegraCustoTest.propriedades()));

        when(repo.empresaExiste("C01")).thenReturn(true);
        when(repo.departamentoDaEmpresa("C01", "C01-D01")).thenReturn(true);
        when(repo.colaboradorDaEmpresa("C01", "C01-E0001")).thenReturn(true);
        when(repo.precos()).thenReturn(List.of(
                mapa("id", "M01", "preco_entrada", new BigDecimal("0.005"), "preco_saida", new BigDecimal("0.015")),
                mapa("id", "M03", "preco_entrada", new BigDecimal("0.015"), "preco_saida", new BigDecimal("0.075"))));
        when(repo.departamentos("C01")).thenReturn(List.of(
                mapa("id", "C01-D01", "nome", "Engenharia de Software"),
                mapa("id", "C01-D02", "nome", "DevOps e Infraestrutura")));
        when(repo.colaboradores("C01")).thenReturn(List.of(
                mapa("id", "C01-E0001", "cargo", "Engenheiro(a) DevOps", "departamento", "C01-D02"),
                mapa("id", "C01-E0002", "cargo", "Analista Financeiro", "departamento", "C01-D01")));
        when(repo.ferramentas()).thenReturn(List.of(
                mapa("id", "T01", "nome", "ChatGPT Enterprise"), mapa("id", "T02", "nome", "Claude for Work")));
        usos(Agrupamento.TOTAL, linha("total", "M01", false, 2, 1000, 2000), linha("total", "M03", true, 1, 2000, 1000));
        usos(Agrupamento.DEPARTAMENTO, linha("C01-D01", "M01", false, 2, 1000, 2000),
                linha("C01-D02", "M03", true, 1, 2000, 1000));
        usos(Agrupamento.COLABORADOR, linha("C01-E0002", "M01", false, 2, 1000, 2000),
                linha("C01-E0001", "M03", true, 1, 2000, 1000));
        usos(Agrupamento.FERRAMENTA, linha("T01", "M01", false, 2, 1000, 2000), linha("T02", "M03", true, 1, 2000, 1000));
    }

    private static Map<String, Object> linha(String id, String modelo, boolean agente, long usos, long entrada, long saida) {
        return mapa("id", id, "modelo", modelo, "agente", agente, "usos", usos,
                "tokens_entrada", BigDecimal.valueOf(entrada), "tokens_saida", BigDecimal.valueOf(saida));
    }

    @SafeVarargs
    private void usos(Agrupamento agrupamento, Map<String, Object>... linhas) {
        when(repo.uso(any(FiltroCusto.class), eq(agrupamento))).thenReturn(new ArrayList<>(List.of(linhas)));
    }

    private static CustoRequest pedido(String departamento, String colaborador) {
        return new CustoRequest("C01", "2026-07-01", "2026-07-31", departamento, colaborador);
    }

    private static void assertValor(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), () -> "esperado " + esperado + ", veio " + atual);
    }

    private FiltroCusto filtroUsado() {
        ArgumentCaptor<FiltroCusto> filtro = ArgumentCaptor.forClass(FiltroCusto.class);
        verify(repo).uso(filtro.capture(), eq(Agrupamento.TOTAL));
        return filtro.getValue();
    }

    // Cenário BDD 1: gestor escolhe empresa e período, clica em "Calcular" e vê o custo total, inclusive de agentes
    @Test
    void bdd1DeveSomarOCustoDeCadaUsoRegistradoInclusiveDeAgentes() {
        CustoResponse r = service.calcular(pedido(null, null));

        assertValor("0.14", r.total());
        assertEquals("USD", r.moeda());
        assertEquals(3, r.usosCobrados());
        assertEquals(0, r.usosSemPreco());
        assertEquals(3000, r.tokensEntrada());
        assertEquals(3000, r.tokensSaida());
        assertNull(r.mensagem());
        assertEquals("custos-v1-preliminar", r.versaoRegra());
        assertEquals("2026-10-22", r.vigencia());
        assertTrue(r.formula().contains("input_tokens / 1000"));
        assertEquals(List.of("success"), r.statusCobrados());
        assertEquals(Map.of(), r.indisponiveis());

        FiltroCusto f = filtroUsado();
        assertEquals("C01", f.empresa());
        assertEquals(LocalDate.of(2026, 7, 1), f.inicio());
        assertEquals(LocalDate.of(2026, 7, 31), f.fim());
        assertNull(f.departamento());
        assertNull(f.colaborador());
    }

    // Cenário BDD 2: período sem nenhum uso
    @Test
    void bdd2PeriodoSemUsoDeveRetornarCustoZeroEMensagemInformativa() {
        usos(Agrupamento.TOTAL);
        usos(Agrupamento.DEPARTAMENTO);
        usos(Agrupamento.COLABORADOR);
        usos(Agrupamento.FERRAMENTA);

        CustoResponse r = service.calcular(new CustoRequest("C01", "2027-01-01", "2027-01-31", null, null));

        assertValor("0", r.total());
        assertValor("0", r.totalAgentes());
        assertValor("0", r.totalUsoDireto());
        assertEquals(0, r.usosCobrados());
        assertEquals(SEM_USO, r.mensagem());
        assertEquals(List.of(), r.porDepartamento());
        assertEquals(List.of(), r.porColaborador());
        assertEquals(List.of(), r.porFerramenta());
    }

    @Test
    void deveSepararCustoDeAgentesDoUsoDireto() {
        CustoResponse r = service.calcular(pedido(null, null));

        assertValor("0.105", r.totalAgentes());
        assertValor("0.035", r.totalUsoDireto());
        assertValor("0.14", r.totalAgentes().add(r.totalUsoDireto()));
        assertEquals(List.of("Pipeline Interno", "Integracao via Backend Interno", "API Direta"), r.aplicacoesAgente());
        assertEquals(List.of("Pipeline Interno", "Integracao via Backend Interno", "API Direta"),
                filtroUsado().aplicacoesAgente());
    }

    @Test
    void deveDetalharPorDepartamentoColaboradorEFerramentaDoMaiorParaOMenorCusto() {
        CustoResponse r = service.calcular(pedido(null, null));

        CustoItemResponse dep = r.porDepartamento().get(0);
        assertEquals("C01-D02", dep.id());
        assertEquals("DevOps e Infraestrutura", dep.nome());
        assertValor("0.105", dep.custo());
        assertEquals(1, dep.usos());
        assertEquals("C01-D01", r.porDepartamento().get(1).id());

        assertEquals("C01-E0001", r.porColaborador().get(0).id());
        assertEquals("C01-E0001 – Engenheiro(a) DevOps", r.porColaborador().get(0).nome());
        assertValor("0.035", r.porColaborador().get(1).custo());

        assertEquals("Claude for Work", r.porFerramenta().get(0).nome());
        assertEquals("ChatGPT Enterprise", r.porFerramenta().get(1).nome());
    }

    @Test
    void filtroPorDepartamentoDeveSerRepassadoAConsulta() {
        service.calcular(pedido("C01-D01", null));

        FiltroCusto f = filtroUsado();
        assertEquals("C01-D01", f.departamento());
        assertNull(f.colaborador());
        verify(repo).departamentoDaEmpresa("C01", "C01-D01");
    }

    @Test
    void filtroPorColaboradorDeveSerRepassadoAConsulta() {
        service.calcular(pedido("", "C01-E0001"));

        FiltroCusto f = filtroUsado();
        assertEquals("C01-E0001", f.colaborador());
        assertNull(f.departamento());
        verify(repo).colaboradorDaEmpresa("C01", "C01-E0001");
    }

    @Test
    void soOsStatusConfiguradosEntramNoCalculo() {
        CustosProperties props = new CustosProperties("v", "2026-10-22", "USD", List.of("success", "timeout"),
                List.of(), 10);
        ConsultaPainel db = mock(ConsultaPainel.class);
        when(db.consultar(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        new CustosService(repo, db, catalogo, new RegraCusto(props)).calcular(pedido(null, null));

        assertEquals(List.of("success", "timeout"), filtroUsado().statusCobrados());
    }

    @Test
    void usoSemPrecoNaoViraCustoZeroEEContadoAParte() {
        usos(Agrupamento.TOTAL, linha("total", "M01", false, 2, 1000, 2000), linha("total", "M03", true, 1, 2000, 1000),
                linha("total", "M99", false, 4, 500, 500), linha("total", null, true, 1, 100, 100));

        CustoResponse r = service.calcular(pedido(null, null));

        assertValor("0.14", r.total());
        assertEquals(3, r.usosCobrados());
        assertEquals(5, r.usosSemPreco());
        assertEquals(3000, r.tokensEntrada());
        assertTrue(r.mensagem().contains("5 usos sem preço"), r.mensagem());
    }

    @Test
    void periodoSoComUsosSemPrecoNaoInformaQueNaoHouveUso() {
        usos(Agrupamento.TOTAL, linha("total", "M99", false, 4, 500, 500));

        CustoResponse r = service.calcular(pedido(null, null));

        assertEquals(4, r.usosSemPreco());
        assertTrue(r.mensagem().contains("4 usos sem preço"), r.mensagem());
    }

    @Test
    void itemDoDetalheTambemContaUsosSemPreco() {
        usos(Agrupamento.FERRAMENTA, linha("T01", "M01", false, 2, 1000, 2000), linha("T09", "M99", false, 3, 10, 10));

        CustoResponse r = service.calcular(pedido(null, null));

        CustoItemResponse semPreco = r.porFerramenta().get(1);
        assertEquals("T09", semPreco.id());
        assertEquals("T09", semPreco.nome());
        assertValor("0", semPreco.custo());
        assertEquals(3, semPreco.usosSemPreco());
    }

    @Test
    void rankingDeColaboradoresRespeitaOLimiteConfigurado() {
        CustosProperties props = new CustosProperties("v", "2026-10-22", "USD", List.of("success"), List.of(), 1);
        ConsultaPainel db = mock(ConsultaPainel.class);
        when(db.consultar(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());

        CustoResponse r = new CustosService(repo, db, catalogo, new RegraCusto(props)).calcular(pedido(null, null));

        assertEquals(1, r.porColaborador().size());
        assertEquals("C01-E0001", r.porColaborador().get(0).id());
    }

    @Test
    void inicioDepoisDoFimDeveSerRejeitado() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(new CustoRequest("C01", "2026-08-01", "2026-07-01", null, null)));
        assertEquals("Data de início deve ser anterior ou igual à data de fim", e.getMessage());
        verify(repo, never()).uso(any(), any());
    }

    @Test
    void empresaAusenteDeveSerRejeitada() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(new CustoRequest(" ", "2026-07-01", "2026-07-31", null, null)));
        assertEquals("Empresa obrigatória", e.getMessage());
    }

    @Test
    void datasAusentesOuInvalidasDevemSerRejeitadas() {
        assertEquals("Data de início obrigatória", assertThrows(IllegalArgumentException.class,
                () -> service.calcular(new CustoRequest("C01", null, "2026-07-31", null, null))).getMessage());
        assertEquals("Data de fim obrigatória", assertThrows(IllegalArgumentException.class,
                () -> service.calcular(new CustoRequest("C01", "2026-07-01", "", null, null))).getMessage());
        assertEquals("Data de início inválida (use AAAA-MM-DD)", assertThrows(IllegalArgumentException.class,
                () -> service.calcular(new CustoRequest("C01", "2026-13-01", "2026-07-31", null, null))).getMessage());
    }

    @Test
    void departamentoEColaboradorJuntosDevemSerRejeitados() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(pedido("C01-D01", "C01-E0001")));
        assertEquals("Informe apenas um filtro: departamento ou colaborador", e.getMessage());
    }

    @Test
    void colaboradorDeOutraEmpresaDeveSerRejeitado() {
        when(repo.colaboradorDaEmpresa("C01", "C02-E0001")).thenReturn(false);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(pedido(null, "C02-E0001")));
        assertEquals("Colaborador não pertence à empresa informada", e.getMessage());
        verify(repo, never()).uso(any(), any());
    }

    @Test
    void departamentoDeOutraEmpresaDeveSerRejeitado() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(pedido("C02-D01", null)));
        assertEquals("Departamento não pertence à empresa informada", e.getMessage());
    }

    @Test
    void empresaInexistenteDeveSerRejeitada() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(new CustoRequest("C99", "2026-07-01", "2026-07-31", null, null)));
        assertEquals("Empresa não encontrada: C99", e.getMessage());
    }

    @Test
    void tabelasAusentesDevemDeixarOCalculoIndisponivelSemQuebrar() {
        when(catalogo.faltantes(anyCollection())).thenReturn(List.of("ai_models"));

        CustoResponse r = service.calcular(pedido(null, null));

        assertNull(r.total());
        assertTrue(r.indisponiveis().get("custos").contains("ai_models"));
        assertTrue(r.mensagem().contains("ai_models"), r.mensagem());
        assertEquals("custos-v1-preliminar", r.versaoRegra());
        verify(repo, never()).uso(any(), any());
    }

    @Test
    void semCadastroDeFerramentasOIdViraONome() {
        when(catalogo.faltantes(anyCollection())).thenAnswer(i -> {
            java.util.Collection<?> req = i.getArgument(0);
            return req.contains("ferramentas.nome") ? List.of("ai_tools") : List.of();
        });

        CustoResponse r = service.calcular(pedido(null, null));

        assertEquals("T02", r.porFerramenta().get(0).nome());
        verify(repo, never()).ferramentas();
    }

    @Test
    void opcoesDevemListarEmpresasDepartamentosColaboradoresEPeriodo() {
        when(repo.empresas()).thenReturn(List.of(mapa("id", "C01", "nome", "NexoCode Solucoes"),
                mapa("id", "C02", "nome", "Vortice Software")));
        when(repo.periodo()).thenReturn(mapa("inicio", "2025-06-01", "fim", "2026-07-31"));

        CustoOpcoesResponse o = service.opcoes("C01");

        assertEquals(2, o.empresas().size());
        assertEquals("NexoCode Solucoes", o.empresas().get(0).nome());
        assertEquals("DevOps e Infraestrutura", o.departamentos().get(1).nome());
        assertEquals("C01-E0001 – Engenheiro(a) DevOps", o.colaboradores().get(0).nome());
        assertEquals("C01-D02", o.colaboradores().get(0).departamentoId());
        assertEquals(LocalDate.of(2025, 6, 1), o.dataMinima());
        assertEquals(LocalDate.of(2026, 7, 31), o.dataMaxima());
        assertEquals(Map.of(), o.indisponiveis());
        verify(catalogo).atualizar();
    }

    @Test
    void opcoesSemEmpresaTrazemSoAsEmpresas() {
        when(repo.empresas()).thenReturn(List.of(mapa("id", "C01", "nome", "NexoCode Solucoes")));
        when(repo.periodo()).thenReturn(mapa("inicio", null, "fim", null));

        CustoOpcoesResponse o = service.opcoes(null);

        assertEquals(1, o.empresas().size());
        assertEquals(List.of(), o.departamentos());
        assertEquals(List.of(), o.colaboradores());
        assertNull(o.dataMaxima());
        verify(repo, never()).departamentos(any());
    }

    @Test
    void opcoesDeEmpresaInexistenteDevemSerRejeitadas() {
        when(repo.empresas()).thenReturn(List.of(mapa("id", "C01", "nome", "NexoCode Solucoes")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.opcoes("C99"));
        assertEquals("Empresa não encontrada: C99", e.getMessage());
    }

    @Test
    void opcoesComTabelasAusentesFicamIndisponiveis() {
        when(catalogo.faltantes(anyCollection())).thenReturn(List.of("companies"));

        CustoOpcoesResponse o = service.opcoes("C01");

        assertEquals(List.of(), o.empresas());
        assertTrue(o.indisponiveis().get("opcoes").contains("companies"));
        verify(repo, never()).empresas();
    }
}
