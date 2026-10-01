package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    List<Admin> findByEmpresaId(UUID empresaId);
}