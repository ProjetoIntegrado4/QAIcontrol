package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.EmpresaDadosVO;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.dto.*;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmpresaService implements EmpresaUseCase {
    private final EmpresaRepository repo;

    public EmpresaService(EmpresaRepository repo) {
        this.repo = repo;
    }

    public EmpresaResponse create(EmpresaRequest r) {
        return toDto(repo.save(new Empresa(
            new EmpresaVO(r.name()),
            new EmpresaDadosVO(
                r.area(),
            r.razaoSocial(),
            r.cnpj(),
            r.numeroFuncionarios(),
            r.cep(),
            r.endereco(),
            r.cidade(),
            r.estado(),
            r.adminResponsavel()
            )
        )));
    }

    public List<EmpresaResponse> list() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    public boolean exists(UUID id) {
        return repo.existsById(id);
    }

    private EmpresaResponse toDto(Empresa p) {
        return new EmpresaResponse(
                p.getId(),
                p.getName().value(),
                p.getRazaoSocial(),
                p.getCnpj(),
                p.getNumeroFuncionarios(),
                p.getCep(),
                p.getEndereco(),
                p.getCidade(),
                p.getEstado(),
                p.getAdminResponsavel(),
                p.getArea()
        );
    }
}
