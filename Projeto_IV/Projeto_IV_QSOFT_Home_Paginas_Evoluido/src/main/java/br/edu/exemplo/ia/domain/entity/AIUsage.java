package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.AIUsageVO;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage")
public class AIUsage {
    @Id
    private UUID id;

    private UUID empresaId;

        @Embedded
        @AttributeOverrides({
            @AttributeOverride(name = "tokens", column = @Column(name = "tokens")),
            @AttributeOverride(name = "model", column = @Column(name = "model"))
        })
        private AIUsageVO dados;

    private Instant occurredAt;

    protected AIUsage() {
    }

    public AIUsage(UUID empresaId, long tokens, String model) {
        this(empresaId, new AIUsageVO(tokens, model));
    }

    public AIUsage(UUID empresaId, AIUsageVO dados) {
        if (empresaId == null || dados == null) {
            throw new IllegalArgumentException("Empresa e dados de uso são obrigatórios");
        }
        this.id = UUID.randomUUID();
        this.empresaId = empresaId;
        this.dados = dados;
        this.occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEmpresaId() { return empresaId; }
    public long getTokens() { return dados.tokens(); }
    public String getModel() { return dados.model(); }
    public Instant getOccurredAt() { return occurredAt; }
}
