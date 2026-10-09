package br.edu.exemplo.ia.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainValueObjectsTest {

    @Test
    void deveValidarDadosDeAdminEFuncionariosNosValueObjects() {
        assertThrows(IllegalArgumentException.class, () -> new AdminVO(
                " ", "52998224725", "ana@example.com", 11999990000L, "Gestora",
                "hash-secreto", "ATIVO", "ADMIN"
        ));
        assertThrows(IllegalArgumentException.class, () -> FuncionarioVO.completo(
                "Ana", "52998224725", null, 11999990000L, "Analista",
                "Tecnologia", "Q001", "senha-secreta", "ATIVO"
        ));
        assertDoesNotThrow(() -> new FuncionarioVO("Ana"));
    }

    @Test
    void naoDeveExporCredenciaisNoToStringDosValueObjects() {
        AdminVO admin = new AdminVO(
                "Ana", "52998224725", "ana@example.com", 11999990000L, "Gestora",
                "hash-secreto", "ATIVO", "ADMIN"
        );
        FuncionarioVO funcionario = new FuncionarioVO(
                "Bia", "52998224725", "bia@example.com", 11999990000L, "Analista",
                "Tecnologia", "Q001", "senha-secreta", "ATIVO"
        );

        assertFalse(admin.toString().contains("hash-secreto"));
        assertFalse(funcionario.toString().contains("senha-secreta"));
    }

    @Test
    void deveValidarDadosDeEmpresaEConsumoNosValueObjects() {
        assertThrows(IllegalArgumentException.class, () -> new EmpresaDadosVO(
                "Tecnologia", "QSoft Ltda", "12345678000199", -1,
                "01310-100", "Av. Paulista, 1000", "São Paulo", "SP", "Ana"
        ));
        assertThrows(IllegalArgumentException.class, () -> new AIUsageVO(0, "gpt-model"));
        assertDoesNotThrow(() -> new AIUsageVO(1, "gpt-model"));
    }
}