package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;
import br.edu.exemplo.ia.dto.EmpresaRequest;
import br.edu.exemplo.ia.dto.EmpresaResponse;
import br.edu.exemplo.ia.dto.FuncionarioRequest;
import br.edu.exemplo.ia.dto.FuncionarioResponse;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import br.edu.exemplo.ia.dto.UsageSummaryResponse;
import br.edu.exemplo.ia.service.AdminUseCase;
import br.edu.exemplo.ia.service.EmpresaUseCase;
import br.edu.exemplo.ia.service.FuncionarioUseCase;
import br.edu.exemplo.ia.service.UsageUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Os controllers de cadastro apenas repassam as chamadas para o caso de uso. */
class CrudControllersTest {

    private final UUID empresaId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    @Test
    void adminControllerDeveDelegarParaOCasoDeUso() {
        AdminUseCase useCase = mock(AdminUseCase.class);
        AdminController controller = new AdminController(useCase);
        AdminRequest request = new AdminRequest("Ana", "52998224725", "ana@qsoft.com", 11999999999L, "Gerente",
                empresaId, "Senha@123", "Senha@123", "ATIVO", "TOTAL");
        AdminResponse response = new AdminResponse(id, "Ana", "52998224725", "ana@qsoft.com", 11999999999L,
                empresaId, "QSoft", "TI", "Gerente", "ATIVO", "TOTAL", "C01-A001");
        when(useCase.create(request)).thenReturn(response);
        when(useCase.list(empresaId)).thenReturn(List.of(response));
        when(useCase.getById(id)).thenReturn(response);

        assertSame(response, controller.create(request));
        assertEquals(List.of(response), controller.list(empresaId));
        assertSame(response, controller.getById(id));
    }

    @Test
    void empresaControllerDeveDelegarParaOCasoDeUso() {
        EmpresaUseCase useCase = mock(EmpresaUseCase.class);
        EmpresaController controller = new EmpresaController(useCase);
        EmpresaRequest request = new EmpresaRequest("QSoft", "QSoft LTDA", "11222333000181", 10, "01001000",
                "Rua A, 1", "São Paulo", "SP", "Ana", "TI");
        EmpresaResponse response = new EmpresaResponse(empresaId, "QSoft", "QSoft LTDA", "11222333000181", 10,
                "01001000", "Rua A, 1", "São Paulo", "SP", "Ana", "TI", "C01");
        when(useCase.create(request)).thenReturn(response);
        when(useCase.list()).thenReturn(List.of(response));

        assertSame(response, controller.create(request));
        assertEquals(List.of(response), controller.list());
    }

    @Test
    void funcionarioControllerDeveDelegarParaOCasoDeUso() {
        FuncionarioUseCase useCase = mock(FuncionarioUseCase.class);
        FuncionarioController controller = new FuncionarioController(useCase);
        FuncionarioRequest request = new FuncionarioRequest("Bruno", "52998224725", "bruno@qsoft.com",
                11988887777L, "Dev", "TI", empresaId, "M001", "Senha@123", "ATIVO", null);
        FuncionarioResponse response = new FuncionarioResponse(id, "Bruno", "52998224725", "bruno@qsoft.com",
                11988887777L, "Dev", "TI", empresaId, "QSoft", "TI", "M001", "ATIVO", null, null, false, "C01-E0001");
        when(useCase.create(request)).thenReturn(response);
        when(useCase.list(null)).thenReturn(List.of(response));
        when(useCase.getById(id)).thenReturn(response);

        assertSame(response, controller.create(request));
        assertEquals(List.of(response), controller.list(null));
        assertSame(response, controller.getById(id));
    }

    @Test
    void usageControllerDeveRegistrarEListarConsumos() {
        UsageUseCase useCase = mock(UsageUseCase.class);
        UsageController controller = new UsageController(useCase);
        UsageRequest request = new UsageRequest(empresaId, 500, "gpt-demo");
        UsageResponse response = new UsageResponse(id, empresaId, 500, "gpt-demo", Instant.now(), "INFO", "ok");
        UsageSummaryResponse resumo = new UsageSummaryResponse(id, empresaId, 500, "gpt-demo", Instant.now());
        when(useCase.register(request)).thenReturn(response);
        when(useCase.list()).thenReturn(List.of(resumo));

        assertSame(response, controller.register(request));
        assertEquals(List.of(resumo), controller.list());
        verify(useCase).register(request);
    }
}
