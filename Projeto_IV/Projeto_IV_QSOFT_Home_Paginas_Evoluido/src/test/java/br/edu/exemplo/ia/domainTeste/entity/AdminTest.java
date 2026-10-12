package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.AdminVO;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    private static Empresa empresa() {
        return new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
    }

    @Test
    void deveCriarAdminComTodosOsCampos() {
        Empresa empresa = empresa();
        Admin a = new Admin("Ana", "52998224725", "ana@example.com", 11999990000L, empresa,
                "Gestora", "hash", "ATIVO", "ADMIN");
        assertNotNull(a.getId());
        assertEquals("Ana", a.getNome());
        assertEquals("52998224725", a.getCpf());
        assertEquals("ana@example.com", a.getEmail());
        assertEquals(11999990000L, a.getTelefone());
        assertSame(empresa, a.getEmpresa());
        assertEquals("Gestora", a.getCargo());
        assertEquals("hash", a.getSenhaHash());
        assertEquals("ATIVO", a.getStatusConta());
        assertEquals("ADMIN", a.getNivelAcesso());
    }

    @Test
    void deveCriarAdminAPartirDoVO() {
        AdminVO vo = new AdminVO("Ana", "52998224725", "a@a.com", 2L, "c", "h", "ATIVO", "ADMIN");
        Admin a = new Admin(vo, empresa());
        assertEquals("Ana", a.getNome());
    }

    @Test
    void deveRejeitarDadosOuEmpresaNulos() {
        AdminVO vo = new AdminVO("Ana", "52998224725", "a@a.com", 2L, "c", "h", "ATIVO", "ADMIN");
        assertThrows(IllegalArgumentException.class, () -> new Admin(null, empresa()));
        assertThrows(IllegalArgumentException.class, () -> new Admin(vo, null));
    }

    @Test
    void deveRejeitarDadosInvalidosNoConstrutorCompleto() {
        assertThrows(IllegalArgumentException.class, () -> new Admin(" ", "52998224725", "e", 1L, empresa(), "c", "h", "a", "n"));
    }
}
