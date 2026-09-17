package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    List<Funcionario> findByEmpresaId(UUID empresaId);
}