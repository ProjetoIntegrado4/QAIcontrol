package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Admins cadastrados. A implementação usa a base de dados em CSV (repository.csv). */
public interface AdminRepository {
    Admin save(Admin admin);

    List<Admin> findAll();

    Optional<Admin> findById(UUID id);

    default List<Admin> findByEmpresaId(UUID empresaId) {
        return findAll().stream().filter(a -> a.getEmpresa().getId().equals(empresaId)).toList();
    }
}
