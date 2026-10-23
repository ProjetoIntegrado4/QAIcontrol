package br.edu.exemplo.ia.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Aceite de uma versão de política de uso de IA por um funcionário (US PP-21, RF15/RN15/RD07).
 * Guarda quem aceitou, qual versão e quando. Um funcionário aceita cada versão uma única vez.
 */
@Entity
@Table(name = "aceite_politica",
        uniqueConstraints = @UniqueConstraint(name = "uk_aceite_funcionario_politica",
                columnNames = {"funcionario_id", "politica_id"}))
public class AceitePolitica {
    @Id
    private UUID id;

    // O funcionário fica na base de dados em CSV (fora do banco), então aqui guardamos só o id dele.
    @Column(name = "funcionario_id", nullable = false)
    private UUID funcionarioId;

    // Preenchido só quando o aceite é criado nesta execução; ao ler do banco, use getFuncionarioId().
    @Transient
    private Funcionario funcionario;

    // Aponta para a linha da versão aceita, não para a política "atual".
    @ManyToOne(optional = false)
    @JoinColumn(name = "politica_id", nullable = false)
    private PoliticaUso politica;

    @Column(name = "versao", nullable = false)
    private int versao;

    @Column(name = "data_aceite", nullable = false)
    private Instant dataAceite;

    protected AceitePolitica() {
    }

    public AceitePolitica(Funcionario funcionario, PoliticaUso politica) {
        if (funcionario == null) {
            throw new IllegalArgumentException("Funcionário obrigatório");
        }
        if (politica == null) {
            throw new IllegalArgumentException("Política obrigatória");
        }
        if (!funcionario.getEmpresa().getId().equals(politica.getEmpresaId())) {
            throw new IllegalArgumentException("Funcionário não pertence à empresa da política");
        }
        if (!politica.isVigente()) {
            throw new IllegalArgumentException("Só é possível aceitar a versão vigente da política");
        }
        this.id = UUID.randomUUID();
        this.funcionario = funcionario;
        this.funcionarioId = funcionario.getId();
        this.politica = politica;
        this.versao = politica.getVersao();
        this.dataAceite = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public PoliticaUso getPolitica() {
        return politica;
    }

    public int getVersao() {
        return versao;
    }

    public Instant getDataAceite() {
        return dataAceite;
    }
}
