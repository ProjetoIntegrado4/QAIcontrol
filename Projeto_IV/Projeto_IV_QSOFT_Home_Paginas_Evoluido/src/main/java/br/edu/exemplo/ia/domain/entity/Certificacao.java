package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Curso ou certificação em IA concluído por um funcionário (US PP-19).
 * Cada registro soma pontos de AI Literacy no perfil dele (ver PontuacaoAiLiteracy).
 */
@Entity
@Table(name = "certificacao")
public class Certificacao {
    // Tamanho máximo de nome e instituição (o mesmo da coluna no banco).
    public static final int TAMANHO_MAXIMO = 150;

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "nome", nullable = false, length = TAMANHO_MAXIMO)
    private String nome;

    @Column(name = "instituicao", nullable = false, length = TAMANHO_MAXIMO)
    private String instituicao;

    @Column(name = "data_conclusao", nullable = false)
    private LocalDate dataConclusao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCertificacao tipo;

    @Column(name = "data_criacao", nullable = false)
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
        this.nome = nome.trim();
        this.instituicao = instituicao.trim();
        this.dataConclusao = dataConclusao;
        this.tipo = tipo;
        this.dataCriacao = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Funcionario getFuncionario() {
        return funcionario;
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
