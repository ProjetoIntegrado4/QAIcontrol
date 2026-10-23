package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.AdminVO;
import br.edu.exemplo.ia.domain.vo.CodigoCsv;

import java.util.UUID;

/** Administrador de uma empresa. Fica gravado na base de dados em CSV (admins.csv). */
public class Admin {
    static final String TIPO = "admin";

    private UUID id;

    /** Código do admin na base de dados (admin_id, ex.: C01-A001). Nulo até ser gravado. */
    private String codigo;

    private AdminVO dados;

    private Empresa empresa;

    protected Admin() {
    }

    public Admin(String nome, String cpf, String email, Long telefone, Empresa empresa,
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
        this.dados = dados.validar();
        this.empresa = empresa;
    }

    /** Recria um admin já gravado na base de dados, sem revalidar os campos. */
    public static Admin restaurar(String codigo, AdminVO dados, Empresa empresa) {
        Admin admin = new Admin();
        admin.definirCodigo(codigo);
        admin.dados = dados;
        admin.empresa = empresa;
        return admin;
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

    public String getNome() {
        return dados == null ? null : dados.nome();
    }

    public String getCpf() {
        return dados == null ? null : dados.cpf();
    }

    public String getEmail() {
        return dados == null ? null : dados.email();
    }

    public Long getTelefone() {
        return dados == null ? null : dados.telefone();
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getCargo() {
        return dados == null ? null : dados.cargo();
    }

    public String getSenhaHash() {
        return dados == null ? null : dados.senhaHash();
    }

    public String getStatusConta() {
        return dados == null ? null : dados.statusConta();
    }

    public String getNivelAcesso() {
        return dados == null ? null : dados.nivelAcesso();
    }
}