package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.PoliticaUso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PoliticaUsoRepository extends JpaRepository<PoliticaUso, UUID> {
    // Versões vigentes (uma por política) da empresa, em ordem alfabética.
    List<PoliticaUso> findByEmpresaIdAndVigenteTrueOrderByTituloAsc(UUID empresaId);

    // Histórico de versões de uma política, da mais recente (a vigente) para a mais antiga.
    List<PoliticaUso> findByCodigoOrderByVersaoDesc(UUID codigo);
}
