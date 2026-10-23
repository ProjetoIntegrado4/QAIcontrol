package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.Campos;
import br.edu.exemplo.ia.domain.vo.Cpf;
import br.edu.exemplo.ia.domain.vo.FuncionarioVO;
import br.edu.exemplo.ia.dto.FuncionarioRequest;
import br.edu.exemplo.ia.dto.FuncionarioResponse;
import br.edu.exemplo.ia.repository.AdminRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FuncionarioService implements FuncionarioUseCase {
    private final FuncionarioRepository funcionarioRepository;
    private final EmpresaRepository empresaRepository;
    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public FuncionarioService(FuncionarioRepository funcionarioRepository, EmpresaRepository empresaRepository,
                              AdminRepository adminRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.empresaRepository = empresaRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public FuncionarioResponse create(FuncionarioRequest request) {
        Empresa empresa = loadEmpresa(request.empresaId());
        Admin gestorResponsavel = request.gestorResponsavelId() == null
            ? null
            : loadGestor(request.gestorResponsavelId(), empresa);
        Funcionario funcionario = funcionarioRepository.save(
            new Funcionario(FuncionarioVO.completo(
                Campos.texto(request.nome(), "Nome do funcionário"),
                Cpf.normalizar(request.cpf(), "CPF do funcionário"),
                Campos.email(request.emailCorporativo(), "E-mail corporativo do funcionário"),
                Campos.telefone(request.telefone(), "Telefone do funcionário"),
                Campos.texto(request.cargo(), "Cargo do funcionário"),
                Campos.texto(request.setor(), "Setor do funcionário"),
                Campos.texto(request.matricula(), "Matrícula do funcionário"),
                passwordEncoder.encode(Campos.senha(request.senha(), "Senha do funcionário")),
                Campos.statusConta(request.statusConta(), "Status da conta do funcionário")
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

    private Admin loadGestor(UUID gestorId, Empresa empresa) {
        Admin gestor = adminRepository.findById(gestorId)
                .orElseThrow(() -> new IllegalArgumentException("Gestor responsavel inexistente: " + gestorId));
        if (!gestor.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException("Gestor responsavel deve ser um admin da mesma empresa");
        }
        return gestor;
    }

    private FuncionarioResponse toDto(Funcionario funcionario) {
        Empresa empresa = funcionario.getEmpresa();
        Admin gestorResponsavel = funcionario.getGestorResponsavel();
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getCpf(),
                funcionario.getEmailCorporativo(),
                funcionario.getTelefone(),
                funcionario.getCargo(),
                funcionario.getSetor(),
                empresa.getId(),
                empresa.getNome(),
                empresa.getArea(),
                funcionario.getMatricula(),
                funcionario.getStatusConta(),
                gestorResponsavel == null ? null : gestorResponsavel.getId(),
                gestorResponsavel == null ? null : gestorResponsavel.getNome(),
                funcionario.isPodeGerenciarFuncionarios(),
                funcionario.getCodigo()
        );
    }
}