package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.CertificacaoRequest;
import br.edu.exemplo.ia.dto.CertificacaoResponse;
import br.edu.exemplo.ia.dto.ResumoCertificacaoResponse;
import br.edu.exemplo.ia.service.CertificacaoUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** O controller de capacitações apenas repassa as chamadas para o caso de uso (mesmo padrão do CrudControllersTest). */
class CertificacaoControllerTest {

    @Test
    void certificacaoControllerDeveDelegarParaOCasoDeUso() {
        CertificacaoUseCase useCase = mock(CertificacaoUseCase.class);
        CertificacaoController controller = new CertificacaoController(useCase);
        UUID id = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UUID funcionarioId = UUID.randomUUID();
        CertificacaoRequest request = new CertificacaoRequest(funcionarioId, empresaId, "Fundamentos de IA",
                "Alura", LocalDate.of(2026, 5, 10), "CURSO");
        CertificacaoResponse response = new CertificacaoResponse(id, funcionarioId, "Bruno", "Fundamentos de IA",
                "Alura", LocalDate.of(2026, 5, 10), "CURSO", 10, Instant.now());
        ResumoCertificacaoResponse resumo = new ResumoCertificacaoResponse(funcionarioId, 1, 1, 0, 10);
        when(useCase.registrar(request)).thenReturn(response);
        when(useCase.listar(funcionarioId)).thenReturn(List.of(response));
        when(useCase.resumo(funcionarioId)).thenReturn(resumo);

        assertSame(response, controller.registrar(request));
        assertEquals(List.of(response), controller.listar(funcionarioId));
        assertSame(resumo, controller.resumo(funcionarioId));
    }
}
