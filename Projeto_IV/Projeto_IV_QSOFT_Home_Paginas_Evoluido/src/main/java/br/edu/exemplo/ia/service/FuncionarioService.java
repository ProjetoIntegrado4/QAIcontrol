package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.dto.FuncionarioRequest;
import br.edu.exemplo.ia.dto.FuncionarioResponse;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FuncionarioService implements FuncionarioUseCase {
    private final FuncionarioRepository funcionarioRepository;
    private final EmpresaRepository empresaRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, EmpresaRepository empresaRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public FuncionarioResponse create(FuncionarioRequest request) {
        Empresa empresa = loadEmpresa(request.empresaId());
        Funcionario funcionario = funcionarioRepository.save(
                new Funcionario(request.nome(), empresa, request.cargo())
        );
        return toDto(funcionario);
    }

    @Override
    public List<FuncionarioResponse> list(UUID empresaId) {
        List<Funcionario> funcionarios = empresaId == null
                ? funcionarioRepository.findAll()
                : funcionarioRepository.findByEmpresaId(empresaId);

        return funcionarios.stream().map(this::toDto).toList();
    }

    @Override
    public FuncionarioResponse getById(UUID id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario inexistente: " + id));
        return toDto(funcionario);
    }

    private Empresa loadEmpresa(UUID empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa inexistente: " + empresaId));
    }

    private FuncionarioResponse toDto(Funcionario funcionario) {
        Empresa empresa = funcionario.getEmpresa();
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                empresa.getId(),
                empresa.getName().value(),
                empresa.getArea(),
                funcionario.getCargo(),
                funcionario.isPodeGerenciarFuncionarios()
        );
    }
}