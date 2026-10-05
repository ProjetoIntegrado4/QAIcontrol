package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.AdminVO;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

        @Embedded
        @AttributeOverrides({
            @AttributeOverride(name = "nome", column = @Column(name = "nome")),
            @AttributeOverride(name = "cpf", column = @Column(name = "cpf")),
            @AttributeOverride(name = "email", column = @Column(name = "email")),
            @AttributeOverride(name = "telefone", column = @Column(name = "telefone")),
            @AttributeOverride(name = "cargo", column = @Column(name = "cargo")),
            @AttributeOverride(name = "senhaHash", column = @Column(name = "senha_hash")),
            @AttributeOverride(name = "statusConta", column = @Column(name = "status_conta")),
            @AttributeOverride(name = "nivelAcesso", column = @Column(name = "nivel_acesso"))
        })
        private AdminVO dados;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    protected Admin() {
    }

    public Admin(String nome, Long cpf, String email, Long telefone, Empresa empresa,
                 String cargo, String senhaHash, String statusConta, String nivelAcesso) {
        this(new AdminVO(nome, cpf, email, telefone, cargo, senhaHash, statusConta, nivelAcesso), empresa);
    }

    public Admin(AdminVO dados, Empresa empresa) {
        if (dados == null) {
            throw new IllegalArgumentException("Dados do admin obrigatórios");
        }
        if (empresa == null) {
            throw new IllegalArgumentException("Empresa do admin obrigatória");
        }
        this.id = UUID.randomUUID();
        this.dados = dados;
        this.empresa = empresa;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return dados.nome();
    }

    public Long getCpf() {
        return dados.cpf();
    }

    public String getEmail() {
        return dados.email();
    }

    public Long getTelefone() {
        return dados.telefone();
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getCargo() {
        return dados.cargo();
    }

    public String getSenhaHash() {
        return dados.senhaHash();
    }

    public String getStatusConta() {
        return dados.statusConta();
    }

    public String getNivelAcesso() {
        return dados.nivelAcesso();
    }
}