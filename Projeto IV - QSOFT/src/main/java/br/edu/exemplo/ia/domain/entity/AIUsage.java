package br.edu.exemplo.ia.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage")
public class AIUsage {
    @Id
    private UUID id;
    private UUID projectId;
    private long tokens;
    private String model;
    private Instant occurredAt;

    protected AIUsage() {
    }

    public AIUsage(UUID projectId, long tokens, String model) {
        if (tokens <= 0) throw new IllegalArgumentException("Tokens > 0");
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.tokens = tokens;
        this.model = model;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public long getTokens() {
        return tokens;
    }

    public String getModel() {
        return model;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
