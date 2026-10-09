package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.AdminVO;
import br.edu.exemplo.ia.domain.vo.Cpf;
import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;
import br.edu.exemplo.ia.repository.AdminRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService implements AdminUseCase {
    private final AdminRepository adminRepository;
    private final EmpresaRepository empresaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminService(AdminRepository adminRepository, EmpresaRepository empresaRepository) {
        this.adminRepository = adminRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public AdminResponse create(AdminRequest request) {
        if (!request.senha().equals(request.confirmacaoSenha())) {
            throw new IllegalArgumentException("Senha e confirmacao de senha nao conferem");
        }
        Empresa empresa = loadEmpresa(request.empresaId());
        Admin admin = adminRepository.save(new Admin(new AdminVO(
                request.nome(),
                Cpf.normalizar(request.cpf(), "CPF do admin"),
                request.email(),
            request.telefone(),
                request.cargo(),
                passwordEncoder.encode(request.senha()),
                request.statusConta(),
                request.nivelAcesso()
        ), empresa));
        return toDto(admin);
    }

    @Override
    public List<AdminResponse> list(UUID empresaId) {
        List<Admin> admins = empresaId == null
                ? adminRepository.findAll()
                : adminRepository.findByEmpresaId(empresaId);

        return admins.stream().map(this::toDto).toList();
    }

    @Override
    public AdminResponse getById(UUID id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin inexistente: " + id));
        return toDto(admin);
    }

    private Empresa loadEmpresa(UUID empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa inexistente: " + empresaId));
    }

    private AdminResponse toDto(Admin admin) {
        Empresa empresa = admin.getEmpresa();
        return new AdminResponse(
                admin.getId(),
                admin.getNome(),
                admin.getCpf(),
                admin.getEmail(),
                admin.getTelefone(),
                empresa.getId(),
                empresa.getName().value(),
                empresa.getArea(),
                admin.getCargo(),
                admin.getStatusConta(),
                admin.getNivelAcesso()
        );
    }
}