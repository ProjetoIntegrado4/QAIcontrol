package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Funcionario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Funcionários cadastrados. A implementação usa a base de dados em CSV (repository.csv). */
public interface FuncionarioRepository {
    Funcionario save(Funcionario funcionario);

    List<Funcionario> findAll();

    Optional<Funcionario> findById(UUID id);

    default List<Funcionario> findByEmpresaId(UUID empresaId) {
        return findAll().stream().filter(f -> f.getEmpresa().getId().equals(empresaId)).toList();
    }
}
