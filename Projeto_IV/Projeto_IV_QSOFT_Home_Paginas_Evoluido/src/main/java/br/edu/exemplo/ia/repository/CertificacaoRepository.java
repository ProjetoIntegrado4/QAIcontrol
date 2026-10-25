package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Certificacao;

import java.util.List;
import java.util.UUID;

/**
 * Cursos e certificações dos funcionários. A implementação usa a base de dados em CSV (repository.csv),
 * no mesmo arquivo dos treinamentos já existentes (training_records.csv).
 */
public interface CertificacaoRepository {
    Certificacao save(Certificacao certificacao);

    // Histórico do funcionário, da conclusão mais recente para a mais antiga.
    List<Certificacao> findByFuncionarioIdOrderByDataConclusaoDesc(UUID funcionarioId);
}
