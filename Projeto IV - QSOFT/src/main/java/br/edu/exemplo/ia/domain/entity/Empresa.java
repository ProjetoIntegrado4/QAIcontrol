package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaName;
import jakarta.persistence.*;

@Entity
@Table(name = "ai_project")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Embedded
    private EmpresaName name;
    private String area;

    protected Empresa() {
    }

    public Empresa(EmpresaName name, String area) {
        this.id = id();
        this.name = name;
        this.area = area;
    }

    private long id() {
        return 0;
    }

    public long getId() {
        return id;
    }

    public EmpresaName getName() {
        return name;
    }

    public String getArea() {
        return area;
    }
}
