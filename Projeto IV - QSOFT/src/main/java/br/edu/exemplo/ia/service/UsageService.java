package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import br.edu.exemplo.ia.integration.alert.AlertClient;
import br.edu.exemplo.ia.integration.alert.AlertResponse;
import br.edu.exemplo.ia.repository.UsageRepository;
import org.springframework.stereotype.Service;

@Service
public class UsageService implements UsageUseCase {

    private final UsageRepository usageRepo;
    private final ProjectUseCase projects;
    private final AlertClient alerts;

    public UsageService(
            UsageRepository usageRepo,
            ProjectUseCase projects,
            AlertClient alerts) {
        this.usageRepo = usageRepo;
        this.projects = projects;
        this.alerts = alerts;
    }

    @Override
    public UsageResponse register(UsageRequest request) {
        if (!projects.exists(request.projectId())) {
            throw new IllegalArgumentException("Projeto inexistente");
        }

        AIUsage usage = usageRepo.save(
                new AIUsage(request.projectId(), request.tokens(), request.model())
        );

        // Após persistir o consumo, o monólito chama o serviço Node.js via REST.
        AlertResponse alert = alerts.evaluate(
                usage.getProjectId(),
                usage.getTokens(),
                usage.getModel()
        );

        return new UsageResponse(
                usage.getId(),
                usage.getProjectId(),
                usage.getTokens(),
                usage.getModel(),
                usage.getOccurredAt(),
                alert.level(),
                alert.message()
        );
    }
}
