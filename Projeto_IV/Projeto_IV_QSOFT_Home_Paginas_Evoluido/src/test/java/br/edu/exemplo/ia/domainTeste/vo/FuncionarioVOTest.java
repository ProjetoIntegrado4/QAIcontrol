package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioVOTest {

    private static FuncionarioVO completo() {
        return FuncionarioVO.completo("Bia", "52998224725", "bia@example.com", 11999990000L,
                "Analista", "Tecnologia", "Q001", "senha-secreta", "ATIVO");
    }

    @Test
    void construtorSimplesDeveUsarValoresPadrao() {
        FuncionarioVO vo = new FuncionarioVO("Ana");
        assertEquals("Ana", vo.nome());
        assertEquals("Não informado", vo.cargo());
        assertEquals("ATIVO", vo.statusConta());
        assertNull(vo.cpf());
        assertNull(vo.emailCorporativo());
        assertNull(vo.telefone());
        assertNull(vo.setor());
        assertNull(vo.matricula());
        assertNull(vo.senha());
    }

    @Test
    void completoDeveExporTodosOsCampos() {
        FuncionarioVO vo = completo();
        assertEquals("Bia", vo.nome());
        assertEquals("52998224725", vo.cpf());
        assertEquals("bia@example.com", vo.emailCorporativo());
        assertEquals(11999990000L, vo.telefone());
        assertEquals("Analista", vo.cargo());
        assertEquals("Tecnologia", vo.setor());
        assertEquals("Q001", vo.matricula());
        assertEquals("senha-secreta", vo.senha());
        assertEquals("ATIVO", vo.statusConta());
        assertEquals(completo(), vo);
    }

    @Test
    void deveRejeitarNomeECargoObrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO(null, null, null, null, "c", null, null, null, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, null, null, " ", null, null, null, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO(" ").validar());
    }

    @Test
    void deveRejeitarOpcionaisInvalidosQuandoInformados() {
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, " ", null, "c", null, null, null, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, null, -1L, "c", null, null, null, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, null, null, "c", " ", null, null, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, null, null, "c", null, " ", null, null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, null, null, "c", null, null, " ", null).validar());
        assertThrows(IllegalArgumentException.class, () -> new FuncionarioVO("n", null, null, null, "c", null, null, null, " ").validar());
    }

    @Test
    void completoDeveExigirTodosOsCampos() {
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", null, "e", 1L, "c", "s", "m", "p", "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", " ", "e", 1L, "c", "s", "m", "p", "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", "52998224725", null, 1L, "c", "s", "m", "p", "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", "52998224725", "e", null, "c", "s", "m", "p", "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", "52998224725", "e", 1L, "c", null, "m", "p", "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", "52998224725", "e", 1L, "c", "s", null, "p", "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", "52998224725", "e", 1L, "c", "s", "m", null, "a"));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo("n", "52998224725", "e", 1L, "c", "s", "m", "p", null));
    }

    @Test
    void toStringDeveOcultarSenha() {
        String texto = completo().toString();
        assertTrue(texto.contains("[REDACTED]"));
        assertFalse(texto.contains("senha-secreta"));
    }
}
