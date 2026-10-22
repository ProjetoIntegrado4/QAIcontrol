package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Aceite de política de uso de IA (US PP-21, RF15/RN15/RD07). */
class AceitePoliticaTest {

    private final Empresa empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
    private final Funcionario funcionario = new Funcionario("Ana Souza", empresa, "Analista");
    private final PoliticaUso politica = new PoliticaUso(empresa, "Uso de IA", "Texto", true);

    // CA-15.3 / RN15: o aceite guarda usuário, versão e data
    @Test
    void aceiteDeveGuardarFuncionarioVersaoEData() {
        AceitePolitica aceite = new AceitePolitica(funcionario, politica);

        assertNotNull(aceite.getId());
        assertSame(funcionario, aceite.getFuncionario());
        assertSame(politica, aceite.getPolitica());
        assertEquals(1, aceite.getVersao());
        assertNotNull(aceite.getDataAceite());
    }

    @Test
    void deveExigirFuncionarioEPolitica() {
        IllegalArgumentException semFuncionario = assertThrows(IllegalArgumentException.class,
                () -> new AceitePolitica(null, politica));
        IllegalArgumentException semPolitica = assertThrows(IllegalArgumentException.class,
                () -> new AceitePolitica(funcionario, null));

        assertEquals("Funcionário obrigatório", semFuncionario.getMessage());
        assertEquals("Política obrigatória", semPolitica.getMessage());
    }

    @Test
    void funcionarioDeOutraEmpresaNaoAceita() {
        Funcionario deOutraEmpresa = new Funcionario("Bruno", new Empresa(new EmpresaVO("Outra"), "Saude"), "Dev");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new AceitePolitica(deOutraEmpresa, politica));

        assertEquals("Funcionário não pertence à empresa da política", erro.getMessage());
    }

    @Test
    void soAVersaoVigentePodeSerAceita() {
        politica.novaVersao("Uso de IA", "Texto 2", true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new AceitePolitica(funcionario, politica));

        assertEquals("Só é possível aceitar a versão vigente da política", erro.getMessage());
    }
}
