package br.edu.exemplo.ia.service.risco;

import br.edu.exemplo.ia.dto.FaixaRiscoResponse;
import br.edu.exemplo.ia.dto.RecomendacaoRiscoResponse;
import br.edu.exemplo.ia.dto.RegrasRiscoResponse;
import br.edu.exemplo.ia.dto.UnidadeRiscoRequest;
import br.edu.exemplo.ia.dto.UnidadeRiscoResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static br.edu.exemplo.ia.service.risco.RegraRecomendacaoRiscoTest.RECOMENDACAO_BAIXO;
import static br.edu.exemplo.ia.service.risco.RegraRecomendacaoRiscoTest.RECOMENDACOES_ALTO;
import static br.edu.exemplo.ia.service.risco.RegraRecomendacaoRiscoTest.RECOMENDACOES_CRITICO;
import static br.edu.exemplo.ia.service.risco.RegraRecomendacaoRiscoTest.RECOMENDACOES_MODERADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da Task PS-290 (RF27 – recomendações associadas aos riscos de dependência de IA, US PP-75).
 * Cobrem os critérios de aceite CA-27.1 a CA-27.4, RN17 (versão e vigência) e RN19 (dado insuficiente não vira zero).
 * O Risk Score ainda é calculado só no notebook; aqui ele chega pronto, como entrada.
 */
class RecomendacaoRiscoServiceTest {

    private static final String DADOS_INSUFICIENTES = "Dados insuficientes para calcular o risco";

    private final RecomendacaoRiscoService service =
            new RecomendacaoRiscoService(new RegraRecomendacaoRisco(RegraRecomendacaoRiscoTest.propriedades()));

    private static List<String> todasAsRecomendacoesExceto(List<String> daFaixa) {
        List<String> outras = new ArrayList<>();
        outras.add(RECOMENDACAO_BAIXO);
        outras.addAll(RECOMENDACOES_MODERADO);
        outras.addAll(RECOMENDACOES_ALTO);
        outras.addAll(RECOMENDACOES_CRITICO);
        outras.removeAll(daFaixa);
        return outras;
    }

    // CA-27.1: toda unidade classificada com risco que possua regra apresenta pelo menos uma orientação

    @Test
    void ca271ScoreDeQualquerFaixaTrazPeloMenosUmaRecomendacao() {
        for (String score : List.of("10", "30", "60", "90")) {
            RecomendacaoRiscoResponse r = service.recomendar(score);

            assertFalse(r.recomendacoes().isEmpty(), () -> "score " + score + " sem recomendação");
        }
    }

    @Test
    void ca271TodaFaixaDasRegrasVigentesTemPeloMenosUmaRecomendacao() {
        for (FaixaRiscoResponse faixa : service.regras().faixas()) {
            assertFalse(faixa.recomendacoes().isEmpty(), () -> "faixa " + faixa.nome() + " sem recomendação");
        }
    }

    // CA-27.2: a recomendação corresponde à classificação obtida

    @Test
    void ca272ScoreBaixoTrazAsRecomendacoesDaFaixaBaixo() {
        RecomendacaoRiscoResponse r = service.recomendar("10");

        assertEquals("BAIXO", r.faixa());
        assertEquals(List.of(RECOMENDACAO_BAIXO), r.recomendacoes());
    }

    @Test
    void ca272ScoreModeradoTrazAsRecomendacoesDaFaixaModerado() {
        RecomendacaoRiscoResponse r = service.recomendar("30");

        assertEquals("MODERADO", r.faixa());
        assertEquals(RECOMENDACOES_MODERADO, r.recomendacoes());
    }

    @Test
    void ca272ScoreAltoTrazAsRecomendacoesDaFaixaAlto() {
        RecomendacaoRiscoResponse r = service.recomendar("60");

        assertEquals("ALTO", r.faixa());
        assertEquals(RECOMENDACOES_ALTO, r.recomendacoes());
    }

    @Test
    void ca272ScoreCriticoTrazAsRecomendacoesDaFaixaCritico() {
        RecomendacaoRiscoResponse r = service.recomendar("90");

        assertEquals("CRITICO", r.faixa());
        assertEquals(RECOMENDACOES_CRITICO, r.recomendacoes());
    }

    // CA-27.4: nunca apresenta recomendação incompatível com a faixa calculada

    @Test
    void ca274ScoreModeradoNaoTrazRecomendacaoDeOutraFaixa() {
        List<String> recomendacoes = service.recomendar("49.99").recomendacoes();

        for (String deOutraFaixa : todasAsRecomendacoesExceto(RECOMENDACOES_MODERADO)) {
            assertFalse(recomendacoes.contains(deOutraFaixa), () -> "recomendação de outra faixa: " + deOutraFaixa);
        }
    }

    @Test
    void ca274ScoreCriticoNaoTrazRecomendacaoDeOutraFaixa() {
        List<String> recomendacoes = service.recomendar("75").recomendacoes();

        for (String deOutraFaixa : todasAsRecomendacoesExceto(RECOMENDACOES_CRITICO)) {
            assertFalse(recomendacoes.contains(deOutraFaixa), () -> "recomendação de outra faixa: " + deOutraFaixa);
        }
    }

    // CA-27.3: alteração na classificação atualiza a recomendação

    @Test
    void ca273ScoreQueSobeDeQuarentaParaOitentaMudaFaixaERecomendacoes() {
        RecomendacaoRiscoResponse antes = service.recomendar("40");
        RecomendacaoRiscoResponse depois = service.recomendar("80");

        assertEquals("MODERADO", antes.faixa());
        assertEquals("CRITICO", depois.faixa());
        assertNotEquals(antes.recomendacoes(), depois.recomendacoes());
        assertEquals(RECOMENDACOES_CRITICO, depois.recomendacoes());
    }

    // RN17: versão e vigência da regra em toda resposta

    @Test
    void rn17RespostaTrazVersaoEVigenciaDaRegra() {
        RecomendacaoRiscoResponse r = service.recomendar("60");

        assertEquals("risco-recomendacoes-v1-preliminar", r.versaoRegra());
        assertEquals("2026-10-22", r.vigencia());
        assertEquals(60.0, r.score());
        assertNull(r.mensagem());
    }

    @Test
    void rn17RegrasTrazemVersaoVigenciaCriterioEFaixasComLimites() {
        RegrasRiscoResponse regras = service.regras();

        assertEquals("risco-recomendacoes-v1-preliminar", regras.versaoRegra());
        assertEquals("2026-10-22", regras.vigencia());
        assertFalse(regras.criterio().isBlank());
        assertEquals(List.of("BAIXO", "MODERADO", "ALTO", "CRITICO"),
                regras.faixas().stream().map(FaixaRiscoResponse::nome).toList());
        FaixaRiscoResponse moderado = regras.faixas().get(1);
        assertEquals(25.0, moderado.limiteInferior());
        assertEquals(50.0, moderado.limiteSuperior());
        assertFalse(moderado.limiteSuperiorInclusivo());
        assertTrue(regras.faixas().get(3).limiteSuperiorInclusivo());
        assertEquals(RECOMENDACOES_MODERADO, moderado.recomendacoes());
    }

    // RN19 / CA-23.3: dado insuficiente não vira zero

    @Test
    void rn19SemScoreDevolveDadosInsuficientesSemRecomendacao() {
        RecomendacaoRiscoResponse r = service.recomendar(null);

        assertNull(r.score());
        assertEquals("DADOS_INSUFICIENTES", r.faixa());
        assertEquals(List.of(), r.recomendacoes());
        assertEquals(DADOS_INSUFICIENTES, r.mensagem());
        assertEquals("risco-recomendacoes-v1-preliminar", r.versaoRegra());
    }

    @Test
    void rn19ScoreEmBrancoTambemEhDadosInsuficientes() {
        RecomendacaoRiscoResponse r = service.recomendar("  ");

        assertEquals("DADOS_INSUFICIENTES", r.faixa());
        assertEquals(List.of(), r.recomendacoes());
    }

    // Validações (viram 400 pelo ApiExceptionHandler)

    @Test
    void scoreMenosUmEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.recomendar("-1"));
        assertEquals("Score deve estar entre 0 e 100", erro.getMessage());
    }

    @Test
    void scoreCentoEUmEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.recomendar("101"));
        assertEquals("Score deve estar entre 0 e 100", erro.getMessage());
    }

    @Test
    void scoreQueNaoEhNumeroEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.recomendar("alto"));
        assertEquals("Score inválido: informe um número de 0 a 100", erro.getMessage());
    }

    @Test
    void scoreComDecimalEhAceito() {
        assertEquals("BAIXO", service.recomendar("24.99").faixa());
    }

    // Lote (RD11: unidade, período, versão, score e classificação)

    @Test
    void loteComUnidadesMisturadasClassificaCadaUmaInclusiveSemScore() {
        List<UnidadeRiscoResponse> r = service.recomendarLote(List.of(
                new UnidadeRiscoRequest("C01-E0001", "2026-07", 12.5),
                new UnidadeRiscoRequest("C01-E0002", "2026-07", 80.0),
                new UnidadeRiscoRequest("C01-E0003", "2026-07", null),
                new UnidadeRiscoRequest("C01-E0004", "2026-07", 50.0)));

        assertEquals(4, r.size());
        assertEquals(new UnidadeRiscoResponse("C01-E0001", "2026-07", 12.5, "BAIXO", List.of(RECOMENDACAO_BAIXO),
                "risco-recomendacoes-v1-preliminar"), r.get(0));
        assertEquals("CRITICO", r.get(1).faixa());
        assertEquals(RECOMENDACOES_CRITICO, r.get(1).recomendacoes());
        assertNull(r.get(2).score());
        assertEquals("DADOS_INSUFICIENTES", r.get(2).faixa());
        assertEquals(List.of(), r.get(2).recomendacoes());
        assertEquals("ALTO", r.get(3).faixa());
        assertEquals(RECOMENDACOES_ALTO, r.get(3).recomendacoes());
    }

    @Test
    void loteVazioEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.recomendarLote(List.of()));
        assertEquals("Informe ao menos uma unidade", erro.getMessage());
    }

    @Test
    void loteNuloEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> service.recomendarLote(null));
    }

    @Test
    void loteComItemNuloEhRecusado() {
        List<UnidadeRiscoRequest> comNulo = Arrays.asList(new UnidadeRiscoRequest("C01-E0001", "2026-07", 10.0), null);

        assertThrows(IllegalArgumentException.class, () -> service.recomendarLote(comNulo));
    }

    @Test
    void loteSemUnidadeEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.recomendarLote(List.of(new UnidadeRiscoRequest(" ", "2026-07", 10.0))));
        assertEquals("Unidade obrigatória", erro.getMessage());
    }

    @Test
    void loteSemPeriodoEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.recomendarLote(List.of(new UnidadeRiscoRequest("C01-E0001", null, 10.0))));
        assertEquals("Período obrigatório para a unidade C01-E0001", erro.getMessage());
    }

    @Test
    void loteComScoreForaDaEscalaIdentificaAUnidade() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.recomendarLote(List.of(new UnidadeRiscoRequest("C01-E0001", "2026-07", 10.0),
                        new UnidadeRiscoRequest("C01-E0009", "2026-07", 101.0))));
        assertEquals("Unidade C01-E0009: Score deve estar entre 0 e 100", erro.getMessage());
    }
}
