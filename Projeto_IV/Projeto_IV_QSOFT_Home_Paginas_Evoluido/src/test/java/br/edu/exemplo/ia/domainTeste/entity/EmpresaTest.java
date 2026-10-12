package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaDadosVO;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaTest {

    @Test
    void deveCriarEmpresaSimplesComValoresPadrao() {
        Empresa e = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        assertNotNull(e.getId());
        assertEquals(new EmpresaVO("QSoft"), e.getName());
        assertEquals("Tecnologia", e.getArea());
        assertEquals(0, e.getNumeroFuncionarios());
        assertNull(e.getRazaoSocial());
        assertNull(e.getCnpj());
        assertNull(e.getCep());
        assertNull(e.getEndereco());
        assertNull(e.getCidade());
        assertNull(e.getEstado());
        assertNull(e.getAdminResponsavel());
    }

    @Test
    void deveCriarEmpresaCompleta() {
        Empresa e = new Empresa(new EmpresaVO("QSoft"), "Tecnologia", "QSoft Ltda", "12345678000199", 25,
                "01310-100", "Av. Paulista, 1000", "São Paulo", "SP", "Ana");
        assertEquals("QSoft Ltda", e.getRazaoSocial());
        assertEquals("12345678000199", e.getCnpj());
        assertEquals(25, e.getNumeroFuncionarios());
        assertEquals("01310-100", e.getCep());
        assertEquals("Av. Paulista, 1000", e.getEndereco());
        assertEquals("São Paulo", e.getCidade());
        assertEquals("SP", e.getEstado());
        assertEquals("Ana", e.getAdminResponsavel());
    }

    @Test
    void deveGerarIdsDistintos() {
        Empresa a = new Empresa(new EmpresaVO("A"), "X");
        Empresa b = new Empresa(new EmpresaVO("A"), "X");
        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    void deveRejeitarNomeOuDadosNulos() {
        assertThrows(IllegalArgumentException.class, () -> new Empresa(null, new EmpresaDadosVO("T")));
        assertThrows(IllegalArgumentException.class, () -> new Empresa(new EmpresaVO("A"), (EmpresaDadosVO) null));
    }
}
