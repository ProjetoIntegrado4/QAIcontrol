package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import br.edu.exemplo.ia.domain.vo.CodigoCsv;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Curso ou certificação em IA concluído por um funcionário (US PP-19).
 * Cada registro soma pontos de AI Literacy no perfil dele (ver PontuacaoAiLiteracy).
 * Fica gravado na base de dados em CSV, junto dos treinamentos já existentes (training_records.csv).
 */
public class Certificacao {
    static final String TIPO_REGISTRO = "certificacao";

    // Tamanho máximo de nome e instituição.
    public static final int TAMANHO_MAXIMO = 150;

    private UUID id;

    /** Código do registro na base de dados (training_id, ex.: TR000001). Nulo até ser gravado. */
    private String codigo;

    // O funcionário fica na base de dados em CSV, então aqui guardamos só o id dele.
    private UUID funcionarioId;

    // Preenchido só quando a certificação é criada nesta execução; ao ler da base, use getFuncionarioId().
    private Funcionario funcionario;

    private String nome;

    private String instituicao;

    private LocalDate dataConclusao;

    private TipoCertificacao tipo;

    private Instant dataCriacao;

    protected Certificacao() {
    }

    public Certificacao(Funcionario funcionario, String nome, String instituicao, LocalDate dataConclusao,
                        TipoCertificacao tipo) {
        if (funcionario == null) {
            throw new IllegalArgumentException("Funcionário obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do curso/certificação obrigatório");
        }
        if (nome.trim().length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Nome do curso/certificação deve ter no máximo 150 caracteres");
        }
        if (instituicao == null || instituicao.isBlank()) {
            throw new IllegalArgumentException("Instituição obrigatória");
        }
        if (instituicao.trim().length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Instituição deve ter no máximo 150 caracteres");
        }
        if (dataConclusao == null) {
            throw new IllegalArgumentException("Data de conclusão obrigatória");
        }
        if (dataConclusao.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de conclusão não pode ser futura");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo deve ser CURSO ou CERTIFICACAO");
        }
        this.id = UUID.randomUUID();
        this.funcionario = funcionario;
        this.funcionarioId = funcionario.getId();
        this.nome = nome.trim();
        this.instituicao = instituicao.trim();
        this.dataConclusao = dataConclusao;
        this.tipo = tipo;
        this.dataCriacao = Instant.now();
    }

    /**
     * Recria um registro já gravado na base de dados, sem revalidar os campos
     * (os treinamentos importados podem não ter instituição nem tipo).
     */
    public static Certificacao restaurar(String codigo, String codigoFuncionario, String nome, String instituicao,
                                         LocalDate dataConclusao, TipoCertificacao tipo, Instant dataCriacao) {
        Certificacao certificacao = new Certificacao();
        certificacao.definirCodigo(codigo);
        certificacao.funcionarioId = CodigoCsv.uuid(Funcionario.TIPO, codigoFuncionario);
        certificacao.nome = nome;
        certificacao.instituicao = instituicao;
        certificacao.dataConclusao = dataConclusao;
        certificacao.tipo = tipo;
        certificacao.dataCriacao = dataCriacao;
        return certificacao;
    }

    /** Define o código na base de dados; o id passa a ser derivado dele. */
    public void definirCodigo(String codigo) {
        this.id = CodigoCsv.uuid(TIPO_REGISTRO, codigo);
        this.codigo = codigo.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public String getNome() {
        return nome;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public LocalDate getDataConclusao() {
        return dataConclusao;
    }

    public TipoCertificacao getTipo() {
        return tipo;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }
}
