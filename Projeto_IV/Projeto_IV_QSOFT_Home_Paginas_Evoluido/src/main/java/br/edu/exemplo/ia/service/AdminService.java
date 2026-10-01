package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;
import br.edu.exemplo.ia.repository.AdminRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService implements AdminUseCase {
    private final AdminRepository adminRepository;
    private final EmpresaRepository empresaRepository;

    public AdminService(AdminRepository adminRepository, EmpresaRepository empresaRepository) {
        this.adminRepository = adminRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public AdminResponse create(AdminRequest request) {
        Empresa empresa = loadEmpresa(request.empresaId());
        Admin admin = adminRepository.save(new Admin(request.nome(), empresa, request.cargo()));
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
                empresa.getId(),
                empresa.getName().value(),
                empresa.getArea(),
                admin.getCargo()
        );
    }
}