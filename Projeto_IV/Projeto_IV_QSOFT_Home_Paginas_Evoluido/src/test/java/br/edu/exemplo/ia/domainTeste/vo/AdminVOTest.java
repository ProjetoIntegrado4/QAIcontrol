package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminVOTest {

    private static AdminVO valido() {
        return new AdminVO("Ana", "52998224725", "ana@example.com", 11999990000L,
                "Gestora", "hash-secreto", "ATIVO", "ADMIN");
    }

    @Test
    void deveCriarAdminVOValidoEExporCampos() {
        AdminVO vo = valido();
        assertEquals("Ana", vo.nome());
        assertEquals("52998224725", vo.cpf());
        assertEquals("ana@example.com", vo.email());
        assertEquals(11999990000L, vo.telefone());
        assertEquals("Gestora", vo.cargo());
        assertEquals("hash-secreto", vo.senhaHash());
        assertEquals("ATIVO", vo.statusConta());
        assertEquals("ADMIN", vo.nivelAcesso());
    }

    @Test
    void deveRejeitarCamposTextoNulosOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new AdminVO(null, "52998224725", "e", 1L, "c", "s", "a", "n").validar());
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", " ", 1L, "c", "s", "a", "n").validar());
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", "e", 1L, "", "s", "a", "n").validar());
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", "e", 1L, "c", null, "a", "n").validar());
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", "e", 1L, "c", "s", " ", "n").validar());
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", "e", 1L, "c", "s", "a", null).validar());
    }

    @Test
    void deveRejeitarCpfETelefoneNulosZeradosOuNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", "e", null, "c", "s", "a", "n").validar());
        assertThrows(IllegalArgumentException.class, () -> new AdminVO("n", "52998224725", "e", -5L, "c", "s", "a", "n").validar());
    }

    @Test
    void toStringDeveOcultarSenha() {
        String texto = valido().toString();
        assertTrue(texto.contains("[REDACTED]"));
        assertTrue(texto.contains("Ana"));
        assertFalse(texto.contains("hash-secreto"));
    }

    @Test
    void equalsEHashCodeDevemConsiderarTodosOsCampos() {
        assertEquals(valido(), valido());
        assertEquals(valido().hashCode(), valido().hashCode());
    }
}
