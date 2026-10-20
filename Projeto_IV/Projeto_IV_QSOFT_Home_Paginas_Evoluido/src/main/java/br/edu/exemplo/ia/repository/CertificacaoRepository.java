package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Certificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificacaoRepository extends JpaRepository<Certificacao, UUID> {
    // Histórico do funcionário, da conclusão mais recente para a mais antiga.
    List<Certificacao> findByFuncionarioIdOrderByDataConclusaoDesc(UUID funcionarioId);
}
