package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.CodigoCsv;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.domain.vo.EmpresaDadosVO;

import java.util.UUID;

/** Empresa cadastrada. Fica gravada na base de dados em CSV (companies.csv). */
public class Empresa {
    static final String TIPO = "empresa";

    private UUID id;

    /** Código da empresa na base de dados (company_id, ex.: C01). Nulo até ser gravada. */
    private String codigo;

    private EmpresaVO name;

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
        this.name = name.validar();
        this.dados = dados.validar();
    }

    /** Recria uma empresa já gravada na base de dados, sem revalidar os campos. */
    public static Empresa restaurar(String codigo, EmpresaVO name, EmpresaDadosVO dados) {
        Empresa empresa = new Empresa();
        empresa.definirCodigo(codigo);
        empresa.name = name;
        empresa.dados = dados;
        return empresa;
    }

    /** Define o código na base de dados; o id passa a ser derivado dele. */
    public void definirCodigo(String codigo) {
        this.id = CodigoCsv.uuid(TIPO, codigo);
        this.codigo = codigo.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public EmpresaVO getName() {
        return name;
    }

    public String getNome() {
        return name == null ? null : name.value();
    }

    public String getArea() {
        return dados == null ? null : dados.area();
    }

    public String getRazaoSocial() {
        return dados == null ? null : dados.razaoSocial();
    }

    public String getCnpj() {
        return dados == null ? null : dados.cnpj();
    }

    public int getNumeroFuncionarios() {
        return dados == null || dados.numeroFuncionarios() == null ? 0 : dados.numeroFuncionarios();
    }

    public String getCep() {
        return dados == null ? null : dados.cep();
    }

    public String getEndereco() {
        return dados == null ? null : dados.endereco();
    }

    public String getCidade() {
        return dados == null ? null : dados.cidade();
    }

    public String getEstado() {
        return dados == null ? null : dados.estado();
    }

    public String getAdminResponsavel() {
        return dados == null ? null : dados.adminResponsavel();
    }
}
