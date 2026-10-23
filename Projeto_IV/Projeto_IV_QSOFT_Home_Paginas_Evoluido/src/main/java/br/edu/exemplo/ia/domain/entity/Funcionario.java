package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.CodigoCsv;
import br.edu.exemplo.ia.domain.vo.FuncionarioVO;

import java.util.UUID;

/** Funcionário de uma empresa. Fica gravado na base de dados em CSV (employees.csv). */
public class Funcionario {
    static final String TIPO = "funcionario";

    private UUID id;

    /** Código do funcionário na base de dados (employee_id, ex.: C01-E0001). Nulo até ser gravado. */
    private String codigo;

    private FuncionarioVO dados;

    private boolean podeGerenciarFuncionarios;

    private Empresa empresa;

    // O gestor responsavel e um admin da mesma empresa do funcionario.
    private Admin gestorResponsavel;

    protected Funcionario() {
    }

    public Funcionario(String nome, Empresa empresa, String cargo) {
        this(new FuncionarioVO(nome, null, null, null, cargo, null, null, null, "ATIVO"), empresa, null);
    }

    public Funcionario(String nome, String cpf, String emailCorporativo, Long telefone, String cargo,
                       String setor, Empresa empresa, String matricula, String senha, String statusConta,
                       Admin gestorResponsavel) {
        this(FuncionarioVO.completo(
                nome, cpf, emailCorporativo, telefone, cargo, setor, matricula, senha, statusConta
        ), empresa, gestorResponsavel);
    }

    public Funcionario(FuncionarioVO dados, Empresa empresa, Admin gestorResponsavel) {
        if (dados == null || empresa == null) {
            throw new IllegalArgumentException("Dados do funcionário e empresa são obrigatórios");
        }
        if (gestorResponsavel != null && !gestorResponsavel.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("Gestor responsavel deve ser um admin da mesma empresa");
        }
        this.id = UUID.randomUUID();
        this.dados = dados.validar();
        this.empresa = empresa;
        this.gestorResponsavel = gestorResponsavel;
        this.podeGerenciarFuncionarios = false;
    }

    /** Recria um funcionário já gravado na base de dados, sem revalidar os campos. */
    public static Funcionario restaurar(String codigo, FuncionarioVO dados, Empresa empresa, Admin gestorResponsavel) {
        Funcionario funcionario = new Funcionario();
        funcionario.definirCodigo(codigo);
        funcionario.dados = dados;
        funcionario.empresa = empresa;
        funcionario.gestorResponsavel = gestorResponsavel;
        return funcionario;
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

    public String getEmailCorporativo() {
        return dados == null ? null : dados.emailCorporativo();
    }

    public Long getTelefone() {
        return dados == null ? null : dados.telefone();
    }

    public String getCargo() {
        return dados == null ? null : dados.cargo();
    }

    public String getSetor() {
        return dados == null ? null : dados.setor();
    }

    public String getMatricula() {
        return dados == null ? null : dados.matricula();
    }

    public String getSenha() {
        return dados == null ? null : dados.senha();
    }

    public String getStatusConta() {
        return dados == null || dados.statusConta() == null ? "ATIVO" : dados.statusConta();
    }

    public boolean isPodeGerenciarFuncionarios() {
        return podeGerenciarFuncionarios;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public Admin getGestorResponsavel() {
        return gestorResponsavel;
    }
}