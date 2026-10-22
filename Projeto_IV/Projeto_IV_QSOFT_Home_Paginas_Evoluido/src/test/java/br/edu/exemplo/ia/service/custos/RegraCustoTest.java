package br.edu.exemplo.ia.service.custos;

import br.edu.exemplo.ia.config.CustosProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra de cálculo de custo de IA (US PP-18, PS-60). Valores de referência:
 * 1000 tokens de entrada × 0,005 + 2000 tokens de saída × 0,015 (por 1k tokens) = 0,035 USD.
 */
class RegraCustoTest {

    static CustosProperties propriedades() {
        return new CustosProperties("custos-v1-preliminar", "2026-10-22", "USD", List.of("success"),
                List.of("Pipeline Interno", "Integracao via Backend Interno", "API Direta"), 10);
    }

    private final RegraCusto regra = new RegraCusto(propriedades());

    private static void assertValor(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), () -> "esperado " + esperado + ", veio " + atual);
    }

    @Test
    void custoDeveSerTokensDivididosPorMilVezesOPrecoDeEntradaESaida() {
        assertValor("0.035", regra.custo(1000, 2000, new BigDecimal("0.005"), new BigDecimal("0.015")));
    }

    @Test
    void custoDeveSerExatoParaPrecosPequenos() {
        // 1500 × 0,000075 / 1000 + 500 × 0,0003 / 1000 = 0,0001125 + 0,00015 = 0,0002625
        assertValor("0.0002625", regra.custo(1500, 500, new BigDecimal("0.000075"), new BigDecimal("0.0003")));
    }

    @Test
    void semTokensOCustoEZero() {
        assertValor("0", regra.custo(0, 0, new BigDecimal("0.005"), new BigDecimal("0.015")));
    }

    @Test
    void semPrecoNaoViraCustoZero() {
        assertNull(regra.custo(1000, 2000, null, new BigDecimal("0.015")));
        assertNull(regra.custo(1000, 2000, new BigDecimal("0.005"), null));
    }

    @Test
    void soStatusConfiguradosSaoCobrados() {
        assertTrue(regra.cobrado("success"));
        assertFalse(regra.cobrado("error"));
        assertFalse(regra.cobrado("rate_limited"));
        assertFalse(regra.cobrado(null));
        assertEquals(List.of("success"), regra.statusCobrados());
    }

    @Test
    void aplicacoesConfiguradasContamComoAgente() {
        assertTrue(regra.agente("Pipeline Interno"));
        assertTrue(regra.agente("API Direta"));
        assertFalse(regra.agente("Chat Web"));
        assertFalse(regra.agente(null));
        assertEquals(3, regra.aplicacoesAgente().size());
    }

    @Test
    void deveExporVersaoVigenciaMoedaEFormula() {
        assertEquals("custos-v1-preliminar", regra.versao());
        assertEquals("2026-10-22", regra.vigencia());
        assertEquals("USD", regra.moeda());
        assertEquals(10, regra.limiteColaboradores());
        assertTrue(regra.formula().contains("input_tokens / 1000"));
        assertTrue(regra.formula().contains("output_tokens / 1000"));
        assertTrue(regra.formula().contains("success"));
    }

    @Test
    void arredondaParaQuatroCasas() {
        assertEquals(new BigDecimal("0.1235"), regra.arredondar(new BigDecimal("0.12345")));
        assertEquals(new BigDecimal("0.0000"), regra.arredondar(BigDecimal.ZERO));
    }
}
