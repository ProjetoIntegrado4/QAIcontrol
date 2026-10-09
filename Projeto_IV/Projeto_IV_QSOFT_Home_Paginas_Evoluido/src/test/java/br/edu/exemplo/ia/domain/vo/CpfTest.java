package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfTest {

    @Test
    void deveAceitarCpfComOuSemPontuacao() {
        assertEquals("52998224725", Cpf.normalizar("529.982.247-25", "CPF"));
        assertEquals("12345678901", Cpf.normalizar("12345678901", "CPF"));
        assertEquals("01234567890", Cpf.normalizar("012.345.678-90", "CPF"));
    }

    @Test
    void deveRejeitarCpfVazioOuSemOnzeDigitos() {
        assertThrows(IllegalArgumentException.class, () -> Cpf.normalizar(null, "CPF"));
        assertThrows(IllegalArgumentException.class, () -> Cpf.normalizar(" ", "CPF"));
        assertThrows(IllegalArgumentException.class, () -> Cpf.normalizar("5299822472", "CPF"));
        assertThrows(IllegalArgumentException.class, () -> Cpf.normalizar("529.982.247-255", "CPF"));
    }
}
