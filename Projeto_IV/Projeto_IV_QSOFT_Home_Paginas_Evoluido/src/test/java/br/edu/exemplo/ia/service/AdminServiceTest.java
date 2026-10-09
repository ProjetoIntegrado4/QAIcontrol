package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;
import br.edu.exemplo.ia.repository.AdminRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    @Test
    void deveCadastrarAdminAssociadoAEmpresa() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);

        Empresa empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(adminRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminService service = new AdminService(adminRepository, empresaRepository);
        AdminResponse response = service.create(request(empresa.getId()));

        assertEquals("Ana", response.nome());
        assertEquals("52998224725", response.cpf());
        assertEquals("ana@qsoft.com", response.email());
        assertEquals(11999990000L, response.telefone());
        assertEquals(empresa.getId(), response.empresaId());
        assertEquals("QSoft", response.empresaNome());
        assertEquals("Gestora", response.cargo());
        assertEquals("ATIVO", response.statusConta());
        assertEquals("ADMIN", response.nivelAcesso());
        assertFalse(response.toString().contains("senha"));
        var adminCaptor = forClass(Admin.class);
        verify(adminRepository).save(adminCaptor.capture());
        assertTrue(new BCryptPasswordEncoder().matches("senha-segura", adminCaptor.getValue().getSenhaHash()));
    }

    @Test
    void deveFalharAoCadastrarAdminSemEmpresaExistente() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);

        UUID empresaId = UUID.randomUUID();
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.empty());

        AdminService service = new AdminService(adminRepository, empresaRepository);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request(empresaId))
        );

        assertEquals("Empresa inexistente: " + empresaId, ex.getMessage());
    }

    @Test
    void deveRejeitarSenhaDiferenteDaConfirmacao() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        AdminService service = new AdminService(adminRepository, empresaRepository);
        AdminRequest request = new AdminRequest(
                "Ana", "52998224725", "ana@qsoft.com", 11999990000L, "Gestora",
                UUID.randomUUID(), "senha-segura", "senha-diferente", "ATIVO", "ADMIN"
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(adminRepository, org.mockito.Mockito.never()).save(any(Admin.class));
    }

    @Test
    void deveRejeitarCpfSemOnzeDigitos() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        Empresa empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        AdminService service = new AdminService(adminRepository, empresaRepository);

        AdminRequest request = new AdminRequest(
                "Ana", "123.456", "ana@qsoft.com", 11999990000L, "Gestora",
                empresa.getId(), "senha-segura", "senha-segura", "ATIVO", "ADMIN"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(request));
        assertEquals("CPF do admin deve ter 11 dígitos", ex.getMessage());
        verify(adminRepository, org.mockito.Mockito.never()).save(any(Admin.class));
    }

    private AdminRequest request(UUID empresaId) {
        return new AdminRequest(
                "Ana", "529.982.247-25", "ana@qsoft.com", 11999990000L, "Gestora",
                empresaId, "senha-segura", "senha-segura", "ATIVO", "ADMIN"
        );
    }
}