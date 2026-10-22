package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Política de uso de IA versionada (US PP-21, RF14/RN14/RD07). */
class PoliticaUsoTest {

    private static Empresa empresa() {
        return new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
    }

    @Test
    void devePublicarVersao1VigenteComCodigoEData() {
        Empresa empresa = empresa();

        PoliticaUso p = new PoliticaUso(empresa, "  Uso responsável de IA ", " Não insira dados pessoais. ", true);

        assertNotNull(p.getId());
        assertNotNull(p.getCodigo());
        assertSame(empresa, p.getEmpresa());
        assertEquals("Uso responsável de IA", p.getTitulo());
        assertEquals("Não insira dados pessoais.", p.getConteudo());
        assertEquals(1, p.getVersao());
        assertTrue(p.isObrigatoria());
        assertTrue(p.isVigente());
        assertNotNull(p.getPublicadaEm());
    }

    @Test
    void deveExigirEmpresa() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(null, "Título", "Conteúdo", true));

        assertEquals("Empresa obrigatória", erro.getMessage());
    }

    @Test
    void deveExigirTitulo() {
        IllegalArgumentException tituloEmBranco = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(empresa(), "  ", "Conteúdo", true));
        IllegalArgumentException tituloNulo = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(empresa(), null, "Conteúdo", true));

        assertEquals("Título da política obrigatório", tituloEmBranco.getMessage());
        assertEquals("Título da política obrigatório", tituloNulo.getMessage());
    }

    @Test
    void deveExigirConteudo() {
        IllegalArgumentException conteudoVazio = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(empresa(), "Título", "", true));
        IllegalArgumentException conteudoNulo = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(empresa(), "Título", null, true));

        assertEquals("Conteúdo da política obrigatório", conteudoVazio.getMessage());
        assertEquals("Conteúdo da política obrigatório", conteudoNulo.getMessage());
    }

    @Test
    void deveRejeitarTituloComMaisDe150Caracteres() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(empresa(), "A".repeat(151), "Conteúdo", true));

        assertEquals("Título da política deve ter no máximo 150 caracteres", erro.getMessage());
    }

    @Test
    void deveAceitarTituloComExatamente150Caracteres() {
        PoliticaUso p = new PoliticaUso(empresa(), "A".repeat(150), "Conteúdo", false);

        assertEquals(150, p.getTitulo().length());
    }

    @Test
    void deveRejeitarConteudoComMaisDe20000Caracteres() {
        String conteudoLongo = "A".repeat(PoliticaUso.TAMANHO_MAXIMO_CONTEUDO + 1);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new PoliticaUso(empresa(), "Título", conteudoLongo, true));

        assertEquals("Conteúdo da política deve ter no máximo 20000 caracteres", erro.getMessage());
    }

    // CA-14.3 / RN14: alteração gera nova versão sem excluir a anterior
    @Test
    void novaVersaoDeveManterCodigoEEmpresaEEncerrarAVigenciaDaAnterior() {
        PoliticaUso v1 = new PoliticaUso(empresa(), "Uso de IA", "Texto 1", false);

        PoliticaUso v2 = v1.novaVersao("Uso de IA (revisada)", "Texto 2", true);

        assertNotEquals(v1.getId(), v2.getId());
        assertEquals(v1.getCodigo(), v2.getCodigo());
        assertSame(v1.getEmpresa(), v2.getEmpresa());
        assertEquals(2, v2.getVersao());
        assertEquals("Uso de IA (revisada)", v2.getTitulo());
        assertEquals("Texto 2", v2.getConteudo());
        assertTrue(v2.isObrigatoria());
        assertTrue(v2.isVigente());
        assertFalse(v1.isVigente());
        assertEquals(1, v1.getVersao());
        assertEquals("Texto 1", v1.getConteudo());
    }

    @Test
    void novaVersaoInvalidaNaoDeveEncerrarAVersaoVigente() {
        PoliticaUso v1 = new PoliticaUso(empresa(), "Uso de IA", "Texto 1", true);

        assertThrows(IllegalArgumentException.class, () -> v1.novaVersao("", "Texto 2", true));

        assertTrue(v1.isVigente());
    }

    @Test
    void naoDeveGerarNovaVersaoAPartirDeVersaoSubstituida() {
        PoliticaUso v1 = new PoliticaUso(empresa(), "Uso de IA", "Texto 1", true);
        v1.novaVersao("Uso de IA", "Texto 2", true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> v1.novaVersao("Uso de IA", "Texto 3", true));

        assertEquals("Só a versão vigente pode gerar uma nova versão", erro.getMessage());
    }
}
