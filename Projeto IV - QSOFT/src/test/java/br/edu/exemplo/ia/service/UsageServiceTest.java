package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import br.edu.exemplo.ia.integration.alert.AlertClient;
import br.edu.exemplo.ia.integration.alert.AlertResponse;
import br.edu.exemplo.ia.repository.UsageRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageServiceTest {

    @Test
    void deveRegistrarConsumoEConsultarAlerta_AAA() {
        // Arrange
        UsageRepository repo = mock(UsageRepository.class);
        EmpresaUseCase projects = mock(EmpresaUseCase.class);
        AlertClient alerts = mock(AlertClient.class);

        UUID projectId = UUID.randomUUID();
        when(projects.exists(projectId)).thenReturn(true);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(alerts.evaluate(projectId, 500, "gpt-demo"))
                .thenReturn(new AlertResponse("INFO", "Consumo dentro do esperado"));

        UsageService service = new UsageService(repo, projects, alerts);

        // Act
        UsageResponse result = service.register(
                new UsageRequest(projectId, 500, "gpt-demo")
        );

        // Assert
        assertEquals(500, result.tokens());
        assertEquals("INFO", result.alertLevel());
        verify(repo).save(any(AIUsage.class));
        verify(alerts).evaluate(projectId, 500, "gpt-demo");
    }
}
