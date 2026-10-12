package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.domain.vo.FuncionarioVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioTest {

    private static Empresa empresa() {
        return new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
    }

    @Test
    void deveCriarFuncionarioSimples() {
        Empresa empresa = empresa();
        Funcionario f = new Funcionario("Ana", empresa, "Analista");
        assertNotNull(f.getId());
        assertEquals("Ana", f.getNome());
        assertEquals("Analista", f.getCargo());
        assertEquals("ATIVO", f.getStatusConta());
        assertSame(empresa, f.getEmpresa());
        assertNull(f.getGestorResponsavel());
        assertNull(f.getCpf());
        assertNull(f.getEmailCorporativo());
        assertNull(f.getTelefone());
        assertNull(f.getSetor());
        assertNull(f.getMatricula());
        assertNull(f.getSenha());
        assertFalse(f.isPodeGerenciarFuncionarios());
    }

    @Test
    void deveCriarFuncionarioCompletoComGestor() {
        Empresa empresa = empresa();
        Admin gestor = new Admin("Chefe", "12345678902", "chefe@example.com", 11999990002L, empresa,
                "Gerente", "hash", "ATIVO", "ADMIN");
        Funcionario f = new Funcionario("Bia", "52998224725", "bia@example.com", 11999990000L, "Analista",
                "Tecnologia", empresa, "Q001", "senha", "INATIVO", gestor);
        assertEquals("52998224725", f.getCpf());
        assertEquals("bia@example.com", f.getEmailCorporativo());
        assertEquals(11999990000L, f.getTelefone());
        assertEquals("Tecnologia", f.getSetor());
        assertEquals("Q001", f.getMatricula());
        assertEquals("senha", f.getSenha());
        assertEquals("INATIVO", f.getStatusConta());
        assertSame(gestor, f.getGestorResponsavel());
    }

    @Test
    void deveRejeitarGestorAdminDeOutraEmpresa() {
        Admin gestor = new Admin("Chefe", "12345678902", "chefe@outra.com", 11999990002L,
                new Empresa(new EmpresaVO("Outra"), "Servicos"), "Gerente", "hash", "ATIVO", "ADMIN");
        assertThrows(IllegalArgumentException.class, () -> new Funcionario(new FuncionarioVO("Bia"), empresa(), gestor));
    }

    @Test
    void statusDeveSerAtivoQuandoVOnaoInformaStatus() {
        FuncionarioVO vo = new FuncionarioVO("Ana", null, null, null, "Cargo", null, null, null, null);
        Funcionario f = new Funcionario(vo, empresa(), null);
        assertEquals("ATIVO", f.getStatusConta());
    }

    @Test
    void deveRejeitarDadosOuEmpresaNulos() {
        assertThrows(IllegalArgumentException.class, () -> new Funcionario((FuncionarioVO) null, empresa(), null));
        assertThrows(IllegalArgumentException.class, () -> new Funcionario(new FuncionarioVO("Ana"), null, null));
    }

    @Test
    void deveRejeitarCamposObrigatoriosNoConstrutorCompleto() {
        assertThrows(IllegalArgumentException.class, () -> new Funcionario("Ana", null, "e", 1L, "c", "s",
                empresa(), "m", "p", "ATIVO", null));
    }
}
