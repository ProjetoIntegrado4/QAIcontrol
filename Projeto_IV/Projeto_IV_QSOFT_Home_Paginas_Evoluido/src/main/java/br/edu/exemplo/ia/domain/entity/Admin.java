package br.edu.exemplo.ia.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "admin")
public class Admin {
    @Id
    private UUID id;

    private String nome;

    private String cargo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    protected Admin() {
    }

    public Admin(String nome, Empresa empresa, String cargo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do admin obrigatorio");
        }
        if (empresa == null) {
            throw new IllegalArgumentException("Empresa do admin obrigatoria");
        }
        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("Cargo do admin obrigatorio");
        }

        this.id = UUID.randomUUID();
        this.nome = nome;
        this.empresa = empresa;
        this.cargo = cargo;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getCargo() {
        return cargo;
    }
}