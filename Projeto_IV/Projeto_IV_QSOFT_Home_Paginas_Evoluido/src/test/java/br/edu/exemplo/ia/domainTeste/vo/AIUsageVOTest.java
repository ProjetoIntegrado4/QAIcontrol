package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIUsageVOTest {

    @Test
    void deveCriarVOValido() {
        AIUsageVO vo = new AIUsageVO(10, "gpt-model");
        assertEquals(10, vo.tokens());
        assertEquals("gpt-model", vo.model());
        assertEquals(new AIUsageVO(10, "gpt-model"), vo);
    }

    @Test
    void deveRejeitarTokensNaoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new AIUsageVO(0, "m").validar());
        assertThrows(IllegalArgumentException.class, () -> new AIUsageVO(-1, "m").validar());
    }

    @Test
    void deveRejeitarModeloNuloOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new AIUsageVO(1, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new AIUsageVO(1, "  ").validar());
    }
}
