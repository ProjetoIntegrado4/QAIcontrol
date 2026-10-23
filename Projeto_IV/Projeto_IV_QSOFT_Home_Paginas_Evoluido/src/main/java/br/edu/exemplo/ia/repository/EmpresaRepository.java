package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Empresa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Empresas cadastradas. A implementação usa a base de dados em CSV (repository.csv). */
public interface EmpresaRepository {
    Empresa save(Empresa empresa);

    List<Empresa> findAll();

    Optional<Empresa> findById(UUID id);

    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
}
