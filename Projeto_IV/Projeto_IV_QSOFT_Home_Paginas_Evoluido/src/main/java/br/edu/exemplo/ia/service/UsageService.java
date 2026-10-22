package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.domain.vo.AIUsageVO;
import br.edu.exemplo.ia.domain.vo.Campos;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import br.edu.exemplo.ia.dto.UsageSummaryResponse;
import br.edu.exemplo.ia.integration.alert.AlertClient;
import br.edu.exemplo.ia.integration.alert.AlertResponse;
import br.edu.exemplo.ia.repository.UsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsageService implements UsageUseCase {

    private final UsageRepository usageRepo;
    private final EmpresaUseCase empresas;
    private final AlertClient alerts;
    private final PoliticaUseCase politicas;

    /** Construtor anterior à US PP-21, sem verificação de políticas: só registra uso sem colaborador. */
    public UsageService(UsageRepository usageRepo, EmpresaUseCase empresas, AlertClient alerts) {
        this(usageRepo, empresas, alerts, null);
    }

    @Autowired
    public UsageService(UsageRepository usageRepo, EmpresaUseCase empresas, AlertClient alerts,
                        PoliticaUseCase politicas) {
        this.usageRepo = usageRepo;
        this.empresas = empresas;
        this.alerts = alerts;
        this.politicas = politicas;
    }

    @Override
    public UsageResponse register(UsageRequest request) {
        if (!empresas.exists(request.empresaId())) {
            throw new IllegalArgumentException("Empresa inexistente: " + request.empresaId());
        }
        if (request.funcionarioId() != null) {
            exigirAceiteDasPoliticas(request);
        }

        AIUsage usage = usageRepo.save(
            new AIUsage(request.empresaId(), new AIUsageVO(request.tokens(), Campos.texto(request.model(), "Modelo")))
        );

        AlertResponse alert = alerts.evaluate(
                usage.getEmpresaId(), usage.getTokens(), usage.getModel()
        );

        return new UsageResponse(
                usage.getId(), usage.getEmpresaId(), usage.getTokens(), usage.getModel(),
                usage.getOccurredAt(), alert.level(), alert.message()
        );
    }

    @Override
    public List<UsageSummaryResponse> list() {
        return usageRepo.findAll().stream()
                .map(u -> new UsageSummaryResponse(
                        u.getId(), u.getEmpresaId(), u.getTokens(), u.getModel(), u.getOccurredAt()))
                .toList();
    }

    // US PP-21 (BDD 2): colaborador com política obrigatória vigente sem aceite não registra uso de IA.
    private void exigirAceiteDasPoliticas(UsageRequest request) {
        if (politicas == null) {
            throw new IllegalStateException("Verificação de políticas de uso de IA não configurada");
        }
        List<PoliticaResponse> pendentes = politicas.pendentes(request.empresaId(), request.funcionarioId());
        if (!pendentes.isEmpty()) {
            throw new AceitePendenteException(pendentes);
        }
    }
}
