package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.ProjectName;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ai_project")
public class AIProject {
    @Id
    private UUID id;
    @Embedded
    private ProjectName name;
    private String area;

    protected AIProject() {
    }

    public AIProject(ProjectName name, String area) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.area = area;
    }

    public UUID getId() {
        return id;
    }

    public ProjectName getName() {
        return name;
    }

    public String getArea() {
        return area;
    }
}
