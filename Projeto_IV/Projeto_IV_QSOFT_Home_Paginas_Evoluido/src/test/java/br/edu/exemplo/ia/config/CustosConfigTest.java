package br.edu.exemplo.ia.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Configuração da regra de custos (application.yml, seção custos) e mapeamento das tabelas usadas no cálculo. */
class CustosConfigTest {

    @Test
    void applicationYmlDeveTrazerARegraPreliminar() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(env.getPropertySources()::addFirst);
        CustosProperties p = Binder.get(env).bind("custos", CustosProperties.class).get();

        assertEquals("custos-v1-preliminar", p.versaoRegra());
        assertNotNull(p.vigencia());
        assertEquals("USD", p.moeda());
        assertEquals(List.of("success"), p.statusCobrados());
        assertEquals(List.of("Pipeline Interno", "Integracao via Backend Interno", "API Direta"), p.aplicacoesAgente());
        assertEquals(10, p.limiteColaboradores());
    }

    @Test
    void produtividadeYmlDeveMapearUsoComModeloEPrecosDosModelos() {
        ProdutividadeProperties p = ProdutividadeTestes.propriedades();

        assertEquals("ai_usage_logs", p.tabelas().get("uso_custo").nome());
        assertEquals("model_id", p.tabelas().get("uso_custo").colunas().get("modelo"));
        assertEquals("application", p.tabelas().get("uso_custo").colunas().get("aplicacao"));
        assertEquals("ai_models", p.tabelas().get("modelos").nome());
        assertEquals("input_price_usd_per_1k_tokens", p.tabelas().get("modelos").colunas().get("preco_entrada"));
        assertEquals("output_price_usd_per_1k_tokens", p.tabelas().get("modelos").colunas().get("preco_saida"));
    }

    @Test
    void listasNulasViramVazias() {
        CustosProperties p = new CustosProperties("v", "2026-10-22", "USD", null, null, 10);
        assertEquals(List.of(), p.statusCobrados());
        assertEquals(List.of(), p.aplicacoesAgente());
        new CustosConfig();
    }
}
