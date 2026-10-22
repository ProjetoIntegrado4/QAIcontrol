package br.edu.exemplo.ia.config;

import br.edu.exemplo.ia.config.RiscoProperties.Faixa;
import br.edu.exemplo.ia.service.risco.RegraRecomendacaoRisco;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Configuração da regra de recomendações por faixa de risco (application.yml, seção risco) – PS-290 / RF27. */
class RiscoConfigTest {

    private static RiscoProperties propriedadesDoApplicationYml() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(env.getPropertySources()::addFirst);
        return Binder.get(env).bind("risco", RiscoProperties.class).get();
    }

    private static ApplicationContextRunner contextoComARegra() {
        return new ApplicationContextRunner().withUserConfiguration(RiscoConfig.class, RegraRecomendacaoRisco.class);
    }

    @Test
    void applicationYmlDeveTrazerVersaoEVigenciaDaRegraPreliminar() throws Exception {
        RiscoProperties p = propriedadesDoApplicationYml();

        assertEquals("risco-recomendacoes-v1-preliminar", p.versaoRegra());
        assertDoesNotThrow(() -> LocalDate.parse(p.vigencia()));
    }

    @Test
    void applicationYmlDeveTrazerAsQuatroFaixasDe0a100() throws Exception {
        RiscoProperties p = propriedadesDoApplicationYml();

        assertEquals(List.of(new Faixa("BAIXO", 0.0, 25.0), new Faixa("MODERADO", 25.0, 50.0),
                new Faixa("ALTO", 50.0, 75.0), new Faixa("CRITICO", 75.0, 100.0)), p.faixas());
    }

    @Test
    void applicationYmlDeveTrazerAsRecomendacoesDeCadaFaixa() throws Exception {
        RiscoProperties p = propriedadesDoApplicationYml();

        assertEquals(List.of("Manter as práticas atuais de uso de IA e acompanhar o indicador mensalmente."),
                p.recomendacoes().get("BAIXO"));
        assertEquals(2, p.recomendacoes().get("MODERADO").size());
        assertEquals(3, p.recomendacoes().get("ALTO").size());
        assertEquals("Definir plano de ação com o gestor em até 30 dias.", p.recomendacoes().get("CRITICO").get(0));
    }

    @Test
    void configuracaoDoApplicationYmlSobeARegra() throws Exception {
        RegraRecomendacaoRisco regra = new RegraRecomendacaoRisco(propriedadesDoApplicationYml());

        assertEquals("ALTO", regra.classificar(60.0));
    }

    @Test
    void configuracaoValidaSobeOContexto() {
        contextoComARegra()
                .withPropertyValues("risco.versao-regra=v1", "risco.vigencia=2026-10-22",
                        "risco.faixas[0].nome=BAIXO", "risco.faixas[0].limite-inferior=0", "risco.faixas[0].limite-superior=50",
                        "risco.faixas[1].nome=ALTO", "risco.faixas[1].limite-inferior=50", "risco.faixas[1].limite-superior=100",
                        "risco.recomendacoes.BAIXO[0]=Acompanhar", "risco.recomendacoes.ALTO[0]=Revisar o processo")
                .run(contexto -> {
                    assertNull(contexto.getStartupFailure());
                    assertEquals("ALTO", contexto.getBean(RegraRecomendacaoRisco.class).classificar(100.0));
                });
    }

    @Test
    void configuracaoComBuracoImpedeASubida() {
        contextoComARegra()
                .withPropertyValues("risco.versao-regra=v1", "risco.vigencia=2026-10-22",
                        "risco.faixas[0].nome=BAIXO", "risco.faixas[0].limite-inferior=0", "risco.faixas[0].limite-superior=40",
                        "risco.faixas[1].nome=ALTO", "risco.faixas[1].limite-inferior=50", "risco.faixas[1].limite-superior=100",
                        "risco.recomendacoes.BAIXO[0]=Acompanhar", "risco.recomendacoes.ALTO[0]=Revisar o processo")
                .run(contexto -> {
                    assertNotNull(contexto.getStartupFailure());
                    assertTrue(causaRaiz(contexto.getStartupFailure()).getMessage().contains("buraco"));
                });
    }

    @Test
    void semASecaoRiscoAAplicacaoNaoSobe() {
        contextoComARegra().run(contexto -> assertNotNull(contexto.getStartupFailure()));
    }

    @Test
    void listasEMapasNulosViramVazios() {
        RiscoProperties p = new RiscoProperties("v", "2026-10-22", null, null);

        assertEquals(List.of(), p.faixas());
        assertEquals(Map.of(), p.recomendacoes());
    }

    private static Throwable causaRaiz(Throwable erro) {
        Throwable causa = erro;
        while (causa.getCause() != null) causa = causa.getCause();
        return causa;
    }
}
