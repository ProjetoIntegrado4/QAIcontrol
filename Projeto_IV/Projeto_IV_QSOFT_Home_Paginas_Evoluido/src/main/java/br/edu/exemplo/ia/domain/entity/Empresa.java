package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.domain.vo.EmpresaDadosVO;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    private UUID id;

    @Embedded
    private EmpresaVO name;

        @Embedded
        @AttributeOverrides({
            @AttributeOverride(name = "area", column = @Column(name = "area")),
            @AttributeOverride(name = "razaoSocial", column = @Column(name = "razao_social")),
            @AttributeOverride(name = "cnpj", column = @Column(name = "cnpj")),
            @AttributeOverride(name = "numeroFuncionarios", column = @Column(name = "numero_funcionarios")),
            @AttributeOverride(name = "cep", column = @Column(name = "cep")),
            @AttributeOverride(name = "endereco", column = @Column(name = "endereco")),
            @AttributeOverride(name = "cidade", column = @Column(name = "cidade")),
            @AttributeOverride(name = "estado", column = @Column(name = "estado")),
            @AttributeOverride(name = "adminResponsavel", column = @Column(name = "admin_responsavel"))
        })
        private EmpresaDadosVO dados;

    protected Empresa() {
    }

    public Empresa(EmpresaVO name, String area) {
        this(name, new EmpresaDadosVO(area));
    }

    public Empresa(EmpresaVO name, String area, String razaoSocial, String cnpj, int numeroFuncionarios,
                   String cep, String endereco, String cidade, String estado, String adminResponsavel) {
        this(name, new EmpresaDadosVO(
                area, razaoSocial, cnpj, numeroFuncionarios, cep, endereco, cidade, estado, adminResponsavel
        ));
    }

    public Empresa(EmpresaVO name, EmpresaDadosVO dados) {
        if (name == null || dados == null) {
            throw new IllegalArgumentException("Nome e dados da empresa obrigatórios");
        }
        this.id = UUID.randomUUID();
        this.name = name;
        this.dados = dados;
    }

    public UUID getId() {
        return id;
    }

    public EmpresaVO getName() {
        return name;
    }

    public String getArea() {
        return dados.area();
    }

    public String getRazaoSocial() {
        return dados.razaoSocial();
    }

    public String getCnpj() {
        return dados.cnpj();
    }

    public int getNumeroFuncionarios() {
        return dados.numeroFuncionarios() == null ? 0 : dados.numeroFuncionarios();
    }

    public String getCep() {
        return dados.cep();
    }

    public String getEndereco() {
        return dados.endereco();
    }

    public String getCidade() {
        return dados.cidade();
    }

    public String getEstado() {
        return dados.estado();
    }

    public String getAdminResponsavel() {
        return dados.adminResponsavel();
    }
}
