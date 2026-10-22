package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.AceitePolitica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AceitePoliticaRepository extends JpaRepository<AceitePolitica, UUID> {
    // Histórico de aceites do funcionário (inclui versões que já não estão vigentes), do mais recente ao mais antigo.
    List<AceitePolitica> findByFuncionarioIdOrderByDataAceiteDesc(UUID funcionarioId);

    Optional<AceitePolitica> findByFuncionarioIdAndPoliticaId(UUID funcionarioId, UUID politicaId);
}
