package br.edu.exemplo.ia.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "funcionario")
public class Funcionario {
    @Id
    private UUID id;

    private String nome;

    private String cargo;

    private boolean podeGerenciarFuncionarios;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    protected Funcionario() {
    }

    public Funcionario(String nome, Empresa empresa, String cargo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do funcionario obrigatorio");
        }
        if (empresa == null) {
            throw new IllegalArgumentException("Empresa do funcionario obrigatoria");
        }
        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("Cargo do funcionario obrigatorio");
        }

        this.id = UUID.randomUUID();
        this.nome = nome;
        this.empresa = empresa;
        this.cargo = cargo;
        this.podeGerenciarFuncionarios = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCargo() {
        return cargo;
    }

    public boolean isPodeGerenciarFuncionarios() {
        return podeGerenciarFuncionarios;
    }

    public Empresa getEmpresa() {
        return empresa;
    }
}