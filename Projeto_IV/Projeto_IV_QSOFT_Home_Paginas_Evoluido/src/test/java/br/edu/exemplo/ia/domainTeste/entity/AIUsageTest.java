package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.AIUsageVO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AIUsageTest {

    @Test
    void deveCriarUsoComTokensEModelo() {
        UUID empresaId = UUID.randomUUID();
        Instant antes = Instant.now();
        AIUsage u = new AIUsage(empresaId, 42, "gpt-model");
        assertNotNull(u.getId());
        assertEquals(empresaId, u.getEmpresaId());
        assertEquals(42, u.getTokens());
        assertEquals("gpt-model", u.getModel());
        assertFalse(u.getOccurredAt().isBefore(antes));
    }

    @Test
    void deveCriarUsoAPartirDoVO() {
        AIUsage u = new AIUsage(UUID.randomUUID(), new AIUsageVO(5, "m"));
        assertEquals(5, u.getTokens());
        assertEquals("m", u.getModel());
    }

    @Test
    void deveRejeitarEmpresaOuDadosNulos() {
        assertThrows(IllegalArgumentException.class, () -> new AIUsage(null, new AIUsageVO(1, "m")));
        assertThrows(IllegalArgumentException.class, () -> new AIUsage(UUID.randomUUID(), (AIUsageVO) null));
    }

    @Test
    void deveRejeitarTokensInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new AIUsage(UUID.randomUUID(), 0, "m"));
    }
}
