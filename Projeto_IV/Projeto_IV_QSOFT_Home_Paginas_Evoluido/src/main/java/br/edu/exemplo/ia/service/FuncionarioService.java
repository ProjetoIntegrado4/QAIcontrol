package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.Cpf;
import br.edu.exemplo.ia.domain.vo.FuncionarioVO;
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
        Funcionario gestorResponsavel = request.gestorResponsavelId() == null
            ? null
            : loadGestor(request.gestorResponsavelId(), empresa);
        Funcionario funcionario = funcionarioRepository.save(
            new Funcionario(FuncionarioVO.completo(
                request.nome(),
                Cpf.normalizar(request.cpf(), "CPF do funcionário"),
                request.emailCorporativo(),
                request.telefone(),
                request.cargo(),
                request.setor(),
                request.matricula(),
                request.senha(),
                request.statusConta()
            ), empresa, gestorResponsavel)
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

    private Funcionario loadGestor(UUID gestorId, Empresa empresa) {
        Funcionario gestor = funcionarioRepository.findById(gestorId)
                .orElseThrow(() -> new IllegalArgumentException("Gestor responsavel inexistente: " + gestorId));
        if (!gestor.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("Gestor responsavel deve pertencer a mesma empresa");
        }
        return gestor;
    }

    private FuncionarioResponse toDto(Funcionario funcionario) {
        Empresa empresa = funcionario.getEmpresa();
        Funcionario gestorResponsavel = funcionario.getGestorResponsavel();
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCpf(),
                funcionario.getEmailCorporativo(),
                funcionario.getTelefone(),
                funcionario.getCargo(),
                funcionario.getSetor(),
                empresa.getId(),
                empresa.getName().value(),
                empresa.getArea(),
                funcionario.getMatricula(),
                funcionario.getStatusConta(),
                gestorResponsavel == null ? null : gestorResponsavel.getId(),
                gestorResponsavel == null ? null : gestorResponsavel.getNome(),
                funcionario.isPodeGerenciarFuncionarios()
        );
    }
}