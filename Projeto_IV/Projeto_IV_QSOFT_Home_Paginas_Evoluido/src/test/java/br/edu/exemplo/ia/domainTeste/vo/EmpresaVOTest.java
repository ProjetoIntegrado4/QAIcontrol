package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaVOTest {

    @Test
    void empresaVODeveAceitarNomeValido() {
        assertEquals("QSoft", new EmpresaVO("QSoft").value());
    }

    @Test
    void empresaVODeveRejeitarNomeNuloOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new EmpresaVO(null).validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaVO(" ").validar());
    }

    @Test
    void empresaNameDeveAceitarNomeValido() {
        assertEquals("QSoft", new EmpresaName("QSoft").value());
    }

    @Test
    void empresaNameDeveRejeitarNomeNuloOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new EmpresaName(null).validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaName("").validar());
    }
}
