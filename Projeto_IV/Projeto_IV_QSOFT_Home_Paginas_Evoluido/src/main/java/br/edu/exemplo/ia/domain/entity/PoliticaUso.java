package br.edu.exemplo.ia.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Política de uso responsável de IA de uma empresa (US PP-21, RF14/RD07).
 * Cada linha é uma VERSÃO: as versões da mesma política têm o mesmo código e só a mais recente fica vigente.
 * Versões anteriores nunca são apagadas, para preservar o histórico e os aceites já registrados (RN14/RN15).
 */
@Entity
@Table(name = "politica_uso",
        uniqueConstraints = @UniqueConstraint(name = "uk_politica_codigo_versao", columnNames = {"codigo", "versao"}))
public class PoliticaUso {
    // Tamanhos máximos (os mesmos das colunas no banco).
    public static final int TAMANHO_MAXIMO_TITULO = 150;
    public static final int TAMANHO_MAXIMO_CONTEUDO = 20000;

    @Id
    private UUID id;

    // Agrupa as versões da mesma política.
    @Column(name = "codigo", nullable = false)
    private UUID codigo;

    // A empresa fica na base de dados em CSV (fora do banco), então aqui guardamos só o id dela.
    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    // Preenchida só quando a versão é criada nesta execução; ao ler do banco, use getEmpresaId().
    @Transient
    private Empresa empresa;

    @Column(name = "titulo", nullable = false, length = TAMANHO_MAXIMO_TITULO)
    private String titulo;

    @Column(name = "conteudo", nullable = false, length = TAMANHO_MAXIMO_CONTEUDO)
    private String conteudo;

    @Column(name = "versao", nullable = false)
    private int versao;

    @Column(name = "obrigatoria", nullable = false)
    private boolean obrigatoria;

    @Column(name = "vigente", nullable = false)
    private boolean vigente;

    @Column(name = "publicada_em", nullable = false)
    private Instant publicadaEm;

    protected PoliticaUso() {
    }

    /** Publica a versão 1 de uma nova política. */
    public PoliticaUso(Empresa empresa, String titulo, String conteudo, boolean obrigatoria) {
        this(UUID.randomUUID(), 1, empresa == null ? null : empresa.getId(), empresa, titulo, conteudo, obrigatoria);
    }

    private PoliticaUso(UUID codigo, int versao, UUID empresaId, Empresa empresa, String titulo, String conteudo,
                        boolean obrigatoria) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa obrigatória");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título da política obrigatório");
        }
        if (titulo.trim().length() > TAMANHO_MAXIMO_TITULO) {
            throw new IllegalArgumentException("Título da política deve ter no máximo 150 caracteres");
        }
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("Conteúdo da política obrigatório");
        }
        if (conteudo.trim().length() > TAMANHO_MAXIMO_CONTEUDO) {
            throw new IllegalArgumentException("Conteúdo da política deve ter no máximo 20000 caracteres");
        }
        this.id = UUID.randomUUID();
        this.codigo = codigo;
        this.versao = versao;
        this.empresaId = empresaId;
        this.empresa = empresa;
        this.titulo = titulo.trim();
        this.conteudo = conteudo.trim();
        this.obrigatoria = obrigatoria;
        this.vigente = true;
        this.publicadaEm = Instant.now();
    }

    /**
     * Publica a próxima versão (CA-14.3): mesmo código e empresa, versão + 1.
     * Esta versão deixa de ser vigente, mas continua existindo. Se os dados novos forem inválidos, nada muda.
     */
    public PoliticaUso novaVersao(String titulo, String conteudo, boolean obrigatoria) {
        if (!vigente) {
            throw new IllegalArgumentException("Só a versão vigente pode gerar uma nova versão");
        }
        PoliticaUso proxima = new PoliticaUso(codigo, versao + 1, empresaId, empresa, titulo, conteudo, obrigatoria);
        this.vigente = false;
        return proxima;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCodigo() {
        return codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public int getVersao() {
        return versao;
    }

    public boolean isObrigatoria() {
        return obrigatoria;
    }

    public boolean isVigente() {
        return vigente;
    }

    public Instant getPublicadaEm() {
        return publicadaEm;
    }
}
