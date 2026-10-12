package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaDadosVOTest {

    private static EmpresaDadosVO completo() {
        return new EmpresaDadosVO("Tecnologia", "QSoft Ltda", "12345678000199", 10,
                "01310-100", "Av. Paulista, 1000", "São Paulo", "SP", "Ana");
    }

    @Test
    void deveCriarSomenteComArea() {
        EmpresaDadosVO vo = new EmpresaDadosVO("Tecnologia");
        assertEquals("Tecnologia", vo.area());
        assertNull(vo.razaoSocial());
        assertNull(vo.cnpj());
        assertNull(vo.numeroFuncionarios());
        assertNull(vo.cep());
        assertNull(vo.endereco());
        assertNull(vo.cidade());
        assertNull(vo.estado());
        assertNull(vo.adminResponsavel());
    }

    @Test
    void deveCriarComTodosOsDados() {
        EmpresaDadosVO vo = completo();
        assertEquals("QSoft Ltda", vo.razaoSocial());
        assertEquals("12345678000199", vo.cnpj());
        assertEquals(10, vo.numeroFuncionarios());
        assertEquals("01310-100", vo.cep());
        assertEquals("Av. Paulista, 1000", vo.endereco());
        assertEquals("São Paulo", vo.cidade());
        assertEquals("SP", vo.estado());
        assertEquals("Ana", vo.adminResponsavel());
        assertEquals(completo(), vo);
    }

    @Test
    void deveAceitarZeroFuncionarios() {
        assertDoesNotThrow(() -> new EmpresaDadosVO("T", "R", "C", 0, "cep", "end", "cid", "UF", "adm"));
    }

    @Test
    void deveRejeitarAreaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO(null).validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO(" ").validar());
    }

    @Test
    void deveRejeitarNumeroFuncionariosNegativoOuNuloComDetalhes() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmpresaDadosVO("T", "R", "C", -1, "cep", "end", "cid", "UF", "adm").validar());
        assertThrows(IllegalArgumentException.class,
                () -> new EmpresaDadosVO("T", "R", "C", null, "cep", "end", "cid", "UF", "adm").validar());
    }

    @Test
    void deveExigirTodosOsCamposQuandoHaDetalhes() {
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", null, "C", 1, "cep", "end", "cid", "UF", "adm").validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", "R", null, 1, "cep", "end", "cid", "UF", "adm").validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", "R", "C", 1, null, "end", "cid", "UF", "adm").validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", "R", "C", 1, "cep", null, "cid", "UF", "adm").validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", "R", "C", 1, "cep", "end", null, "UF", "adm").validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", "R", "C", 1, "cep", "end", "cid", null, "adm").validar());
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", "R", "C", 1, "cep", "end", "cid", "UF", " ").validar());
    }

    @Test
    void deveDetectarDetalhesApenasPeloNumeroDeFuncionarios() {
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO("T", null, null, 5, null, null, null, null, null).validar());
    }
}
