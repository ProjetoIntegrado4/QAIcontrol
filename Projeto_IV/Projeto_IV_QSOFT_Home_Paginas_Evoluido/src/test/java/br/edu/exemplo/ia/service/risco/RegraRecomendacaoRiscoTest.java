package br.edu.exemplo.ia.service.risco;

import br.edu.exemplo.ia.config.RiscoProperties;
import br.edu.exemplo.ia.config.RiscoProperties.Faixa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra de recomendações por faixa de risco de dependência de IA (US PP-75, Task PS-290 / RF27).
 * Faixas de referência (PRELIMINAR): BAIXO 0 a &lt;25, MODERADO 25 a &lt;50, ALTO 50 a &lt;75, CRITICO 75 a 100.
 */
class RegraRecomendacaoRiscoTest {

    static final String RECOMENDACAO_BAIXO =
            "Manter as práticas atuais de uso de IA e acompanhar o indicador mensalmente.";
    static final List<String> RECOMENDACOES_MODERADO = List.of(
            "Reforçar a revisão humana nas entregas produzidas com apoio de IA.",
            "Indicar trilha de capacitação em uso responsável de IA.");
    static final List<String> RECOMENDACOES_ALTO = List.of(
            "Gestor revisar com a equipe o processo em que a IA é utilizada.",
            "Exigir registro de revisão humana nas entregas com IA.",
            "Priorizar capacitação em uso responsável de IA.");
    static final List<String> RECOMENDACOES_CRITICO = List.of(
            "Definir plano de ação com o gestor em até 30 dias.",
            "Revisar o processo e as políticas de uso de IA da área.",
            "Acompanhar o indicador semanalmente até sair da faixa crítica.");

    static List<Faixa> faixas() {
        return List.of(new Faixa("BAIXO", 0.0, 25.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));
    }

    static Map<String, List<String>> recomendacoes() {
        return Map.of("BAIXO", List.of(RECOMENDACAO_BAIXO), "MODERADO", RECOMENDACOES_MODERADO,
                "ALTO", RECOMENDACOES_ALTO, "CRITICO", RECOMENDACOES_CRITICO);
    }

    static RiscoProperties propriedades() {
        return new RiscoProperties("risco-recomendacoes-v1-preliminar", "2026-10-22", faixas(), recomendacoes());
    }

    private final RegraRecomendacaoRisco regra = new RegraRecomendacaoRisco(propriedades());

    private static String erroDeConfiguracao(List<Faixa> faixas, Map<String, List<String>> recomendacoes) {
        RiscoProperties props = new RiscoProperties("risco-recomendacoes-v1-preliminar", "2026-10-22", faixas, recomendacoes);
        return assertThrows(IllegalStateException.class, () -> new RegraRecomendacaoRisco(props)).getMessage();
    }

    // Uma faixa por teste

    @Test
    void scoreDezEstaNaFaixaBaixo() {
        assertEquals("BAIXO", regra.classificar(10.0));
    }

    @Test
    void scoreTrintaEstaNaFaixaModerado() {
        assertEquals("MODERADO", regra.classificar(30.0));
    }

    @Test
    void scoreSessentaEstaNaFaixaAlto() {
        assertEquals("ALTO", regra.classificar(60.0));
    }

    @Test
    void scoreNoventaEstaNaFaixaCritico() {
        assertEquals("CRITICO", regra.classificar(90.0));
    }

    // Limites: inferior inclusivo, superior exclusivo; a última faixa inclui o 100

    @Test
    void scoreZeroEstaNaFaixaBaixo() {
        assertEquals("BAIXO", regra.classificar(0.0));
    }

    @Test
    void scoreVinteEQuatroENoventaENoveAindaEstaNaFaixaBaixo() {
        assertEquals("BAIXO", regra.classificar(24.99));
    }

    @Test
    void scoreVinteECincoJaEstaNaFaixaModerado() {
        assertEquals("MODERADO", regra.classificar(25.0));
    }

    @Test
    void scoreQuarentaENoveENoventaENoveAindaEstaNaFaixaModerado() {
        assertEquals("MODERADO", regra.classificar(49.99));
    }

    @Test
    void scoreCinquentaJaEstaNaFaixaAlto() {
        assertEquals("ALTO", regra.classificar(50.0));
    }

    @Test
    void scoreSetentaEQuatroENoventaENoveAindaEstaNaFaixaAlto() {
        assertEquals("ALTO", regra.classificar(74.99));
    }

    @Test
    void scoreSetentaECincoJaEstaNaFaixaCritico() {
        assertEquals("CRITICO", regra.classificar(75.0));
    }

    @Test
    void scoreCemEstaNaFaixaCritico() {
        assertEquals("CRITICO", regra.classificar(100.0));
    }

    // RN19 / CA-23.3

    @Test
    void scoreAusenteViraDadosInsuficientesENaoZero() {
        assertEquals(RegraRecomendacaoRisco.DADOS_INSUFICIENTES, regra.classificar(null));
    }

    @Test
    void dadosInsuficientesNaoTemRecomendacao() {
        assertEquals(List.of(), regra.recomendacoes(RegraRecomendacaoRisco.DADOS_INSUFICIENTES));
    }

    // Escala 0–100

    @Test
    void scoreNegativoEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> regra.classificar(-1.0));
        assertEquals("Score deve estar entre 0 e 100", erro.getMessage());
    }

    @Test
    void scoreAcimaDeCemEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> regra.classificar(101.0));
        assertEquals("Score deve estar entre 0 e 100", erro.getMessage());
    }

    @Test
    void scoreQueNaoEhNumeroEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> regra.classificar(Double.NaN));
    }

    // CA-27.1 e CA-27.2

    @Test
    void cadaFaixaDevolveAsRecomendacoesConfiguradasParaEla() {
        assertEquals(List.of(RECOMENDACAO_BAIXO), regra.recomendacoes("BAIXO"));
        assertEquals(RECOMENDACOES_MODERADO, regra.recomendacoes("MODERADO"));
        assertEquals(RECOMENDACOES_ALTO, regra.recomendacoes("ALTO"));
        assertEquals(RECOMENDACOES_CRITICO, regra.recomendacoes("CRITICO"));
    }

    // RN17: memória da regra

    @Test
    void deveExporVersaoVigenciaCriterioEFaixasEmOrdem() {
        assertEquals("risco-recomendacoes-v1-preliminar", regra.versao());
        assertEquals("2026-10-22", regra.vigencia());
        assertTrue(regra.criterio().contains("inclusivo"));
        assertTrue(regra.criterio().contains("Dados insuficientes"));
        assertEquals(List.of("BAIXO", "MODERADO", "ALTO", "CRITICO"), regra.faixas().stream().map(Faixa::nome).toList());
    }

    @Test
    void faixasPodemSerConfiguradasForaDeOrdem() {
        List<Faixa> foraDeOrdem = List.of(faixas().get(3), faixas().get(1), faixas().get(0), faixas().get(2));
        RegraRecomendacaoRisco r = new RegraRecomendacaoRisco(
                new RiscoProperties("v", "2026-10-22", foraDeOrdem, recomendacoes()));

        assertEquals("MODERADO", r.classificar(25.0));
        assertEquals("BAIXO", r.faixas().get(0).nome());
    }

    // Configuração inválida falha na subida

    @Test
    void buracoEntreFaixasEhRecusado() {
        List<Faixa> comBuraco = List.of(new Faixa("BAIXO", 0.0, 20.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(comBuraco, recomendacoes()).contains("buraco"));
    }

    @Test
    void sobreposicaoEntreFaixasEhRecusada() {
        List<Faixa> sobrepostas = List.of(new Faixa("BAIXO", 0.0, 30.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(sobrepostas, recomendacoes()).contains("sobreposição"));
    }

    @Test
    void faixasQueNaoComecamEmZeroSaoRecusadas() {
        List<Faixa> semZero = List.of(new Faixa("BAIXO", 5.0, 25.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(semZero, recomendacoes()).contains("começar em 0"));
    }

    @Test
    void faixasQueNaoTerminamEmCemSaoRecusadas() {
        List<Faixa> semCem = List.of(new Faixa("BAIXO", 0.0, 25.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 90.0));

        assertTrue(erroDeConfiguracao(semCem, recomendacoes()).contains("terminar em 100"));
    }

    @Test
    void faixaComLimiteInferiorMaiorOuIgualAoSuperiorEhRecusada() {
        List<Faixa> invertida = List.of(new Faixa("BAIXO", 0.0, 25.0), new Faixa("MODERADO", 50.0, 25.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(invertida, recomendacoes()).contains("MODERADO"));
    }

    @Test
    void faixaSemLimiteEhRecusada() {
        List<Faixa> semLimite = List.of(new Faixa("BAIXO", 0.0, null), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(semLimite, recomendacoes()).contains("BAIXO"));
    }

    @Test
    void faixaComNomeRepetidoEhRecusada() {
        List<Faixa> repetida = List.of(new Faixa("BAIXO", 0.0, 25.0), new Faixa("BAIXO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(repetida, recomendacoes()).contains("repetida"));
    }

    @Test
    void faixaComNomeReservadoDeDadosInsuficientesEhRecusada() {
        List<Faixa> reservada = List.of(new Faixa("DADOS_INSUFICIENTES", 0.0, 25.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0));

        assertTrue(erroDeConfiguracao(reservada, recomendacoes()).contains("DADOS_INSUFICIENTES"));
    }

    @Test
    void semFaixasEhRecusado() {
        assertTrue(erroDeConfiguracao(List.of(), recomendacoes()).contains("nenhuma faixa"));
    }

    @Test
    void recomendacaoDeFaixaInexistenteEhRecusada() {
        Map<String, List<String>> comFaixaInexistente = Map.of("BAIXO", List.of(RECOMENDACAO_BAIXO),
                "MODERADO", RECOMENDACOES_MODERADO, "ALTO", RECOMENDACOES_ALTO, "CRITICO", RECOMENDACOES_CRITICO,
                "EXTREMO", List.of("Recomendação sem faixa"));

        assertTrue(erroDeConfiguracao(faixas(), comFaixaInexistente).contains("EXTREMO"));
    }

    @Test
    void faixaSemRecomendacaoEhRecusada() {
        Map<String, List<String>> semAlto = Map.of("BAIXO", List.of(RECOMENDACAO_BAIXO),
                "MODERADO", RECOMENDACOES_MODERADO, "CRITICO", RECOMENDACOES_CRITICO);

        assertTrue(erroDeConfiguracao(faixas(), semAlto).contains("ALTO"));
    }

    @Test
    void recomendacaoEmBrancoEhRecusada() {
        Map<String, List<String>> emBranco = Map.of("BAIXO", List.of(" "),
                "MODERADO", RECOMENDACOES_MODERADO, "ALTO", RECOMENDACOES_ALTO, "CRITICO", RECOMENDACOES_CRITICO);

        assertTrue(erroDeConfiguracao(faixas(), emBranco).contains("BAIXO"));
    }

    @Test
    void versaoDaRegraAusenteEhRecusada() {
        RiscoProperties semVersao = new RiscoProperties(" ", "2026-10-22", faixas(), recomendacoes());

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> new RegraRecomendacaoRisco(semVersao));
        assertTrue(erro.getMessage().contains("versao-regra"));
    }

    @Test
    void vigenciaInvalidaEhRecusada() {
        RiscoProperties vigenciaInvalida = new RiscoProperties("v", "22/10/2026", faixas(), recomendacoes());

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new RegraRecomendacaoRisco(vigenciaInvalida));
        assertTrue(erro.getMessage().contains("vigencia"));
    }
}
