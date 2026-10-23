package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.Campos;
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
            new EmpresaVO(Campos.texto(r.name(), "Nome da empresa")),
            new EmpresaDadosVO(
                Campos.texto(r.area(), "Área"),
                Campos.texto(r.razaoSocial(), "Razão social"),
                Campos.digitos(r.cnpj(), 14, "CNPJ"),
                r.numeroFuncionarios(),
                Campos.digitos(r.cep(), 8, "CEP"),
                Campos.texto(r.endereco(), "Endereço"),
                Campos.texto(r.cidade(), "Cidade"),
                Campos.uf(r.estado(), "Estado"),
                Campos.texto(r.adminResponsavel(), "Nome do admin responsável")
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
                p.getNome(),
                p.getRazaoSocial(),
                p.getCnpj(),
                p.getNumeroFuncionarios(),
                p.getCep(),
                p.getEndereco(),
                p.getCidade(),
                p.getEstado(),
                p.getAdminResponsavel(),
                p.getArea(),
                p.getCodigo()
        );
    }
}
