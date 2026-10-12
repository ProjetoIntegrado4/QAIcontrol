package br.edu.exemplo.ia.domainTeste.vo;

import br.edu.exemplo.ia.domain.vo.Campos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CamposTest {

    @Test
    void deveNormalizarCamposValidos() {
        assertEquals("Ana Silva", Campos.texto("  Ana Silva ", "Nome"));
        assertEquals("ana@qsoft.com", Campos.email(" Ana@QSoft.com ", "E-mail"));
        assertEquals(11999990000L, Campos.telefone(11999990000L, "Telefone"));
        assertEquals(1133334444L, Campos.telefone(1133334444L, "Telefone"));
        assertEquals("12345678000199", Campos.digitos("12.345.678/0001-99", 14, "CNPJ"));
        assertEquals("01001000", Campos.digitos("01001-000", 8, "CEP"));
        assertEquals("SP", Campos.uf(" sp ", "Estado"));
        assertEquals("ATIVO", Campos.statusConta("ativo", "Status"));
        assertEquals(" senha ", Campos.senha(" senha ", "Senha"));
    }

    @Test
    void deveRejeitarCamposInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Campos.texto(" ", "Nome"));
        assertThrows(IllegalArgumentException.class, () -> Campos.email("ana.qsoft.com", "E-mail"));
        assertThrows(IllegalArgumentException.class, () -> Campos.email("ana@qsoft", "E-mail"));
        assertThrows(IllegalArgumentException.class, () -> Campos.telefone(999990000L, "Telefone"));
        assertThrows(IllegalArgumentException.class, () -> Campos.telefone(null, "Telefone"));
        assertThrows(IllegalArgumentException.class, () -> Campos.digitos("1234", 8, "CEP"));
        assertThrows(IllegalArgumentException.class, () -> Campos.uf("São Paulo", "Estado"));
        assertThrows(IllegalArgumentException.class, () -> Campos.statusConta("BLOQUEADO", "Status"));
        assertThrows(IllegalArgumentException.class, () -> Campos.senha("", "Senha"));
    }
}
