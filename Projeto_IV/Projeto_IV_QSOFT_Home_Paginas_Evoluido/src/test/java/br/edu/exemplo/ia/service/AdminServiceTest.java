package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.EmpresaName;
import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;
import br.edu.exemplo.ia.repository.AdminRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    @Test
    void deveCadastrarAdminAssociadoAEmpresa() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);

        Empresa empresa = new Empresa(new EmpresaName("QSoft"), "Tecnologia");
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(adminRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminService service = new AdminService(adminRepository, empresaRepository);
        AdminResponse response = service.create(new AdminRequest("Ana", empresa.getId(), "Gestora"));

        assertEquals("Ana", response.nome());
        assertEquals(empresa.getId(), response.empresaId());
        assertEquals("QSoft", response.empresaNome());
        assertEquals("Gestora", response.cargo());
        verify(adminRepository).save(any(Admin.class));
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
                () -> service.create(new AdminRequest("Ana", empresaId, "Gestora"))
        );

        assertEquals("Empresa inexistente: " + empresaId, ex.getMessage());
    }
}