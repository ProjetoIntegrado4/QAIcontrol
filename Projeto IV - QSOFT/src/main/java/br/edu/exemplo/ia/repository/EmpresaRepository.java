package br.edu.exemplo.ia.repository;

import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.domain.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;



public interface EmpresaRepository extends JpaRepository<Empresa, AIUsage> {
}