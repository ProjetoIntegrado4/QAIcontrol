package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.FuncionarioVO;
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
@Table(name = "funcionario")
public class Funcionario {
    @Id
    private UUID id;

        @Embedded
        @AttributeOverrides({
            @AttributeOverride(name = "nome", column = @Column(name = "nome")),
            @AttributeOverride(name = "cpf", column = @Column(name = "cpf", length = 11)),
            @AttributeOverride(name = "emailCorporativo", column = @Column(name = "email_corporativo")),
            @AttributeOverride(name = "telefone", column = @Column(name = "telefone")),
            @AttributeOverride(name = "cargo", column = @Column(name = "cargo")),
            @AttributeOverride(name = "setor", column = @Column(name = "setor")),
            @AttributeOverride(name = "matricula", column = @Column(name = "matricula")),
            @AttributeOverride(name = "senha", column = @Column(name = "senha")),
            @AttributeOverride(name = "statusConta", column = @Column(name = "status_conta"))
        })
        private FuncionarioVO dados;

    private boolean podeGerenciarFuncionarios;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "gestor_responsavel_id")
    private Funcionario gestorResponsavel;

    protected Funcionario() {
    }

    public Funcionario(String nome, Empresa empresa, String cargo) {
        this(new FuncionarioVO(nome, null, null, null, cargo, null, null, null, "ATIVO"), empresa, null);
    }

    public Funcionario(String nome, String cpf, String emailCorporativo, Long telefone, String cargo,
                       String setor, Empresa empresa, String matricula, String senha, String statusConta,
                       Funcionario gestorResponsavel) {
        this(FuncionarioVO.completo(
                nome, cpf, emailCorporativo, telefone, cargo, setor, matricula, senha, statusConta
        ), empresa, gestorResponsavel);
    }

    public Funcionario(FuncionarioVO dados, Empresa empresa, Funcionario gestorResponsavel) {
        if (dados == null || empresa == null) {
            throw new IllegalArgumentException("Dados do funcionário e empresa são obrigatórios");
        }
        this.id = UUID.randomUUID();
        this.dados = dados;
        this.empresa = empresa;
        this.gestorResponsavel = gestorResponsavel;
        this.podeGerenciarFuncionarios = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return dados.nome();
    }

    public String getCpf() {
        return dados.cpf();
    }

    public String getEmailCorporativo() {
        return dados.emailCorporativo();
    }

    public Long getTelefone() {
        return dados.telefone();
    }

    public String getCargo() {
        return dados.cargo();
    }

    public String getSetor() {
        return dados.setor();
    }

    public String getMatricula() {
        return dados.matricula();
    }

    public String getSenha() {
        return dados.senha();
    }

    public String getStatusConta() {
        return dados.statusConta() == null ? "ATIVO" : dados.statusConta();
    }

    public boolean isPodeGerenciarFuncionarios() {
        return podeGerenciarFuncionarios;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Funcionario getGestorResponsavel() {
        return gestorResponsavel;
    }
}