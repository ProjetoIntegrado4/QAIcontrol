package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.EmpresaName;
import br.edu.exemplo.ia.dto.FuncionarioRequest;
import br.edu.exemplo.ia.dto.FuncionarioResponse;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FuncionarioServiceTest {

    @Test
    void deveCadastrarFuncionarioAssociadoAEmpresaComAcessoRestrito() {
        FuncionarioRepository funcionarioRepository = mock(FuncionarioRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);

        Empresa empresa = new Empresa(new EmpresaName("QSoft"), "Tecnologia");
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(funcionarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FuncionarioService service = new FuncionarioService(funcionarioRepository, empresaRepository);
        FuncionarioResponse response = service.create(new FuncionarioRequest("Carlos", empresa.getId(), "Analista"));

        assertEquals("Carlos", response.nome());
        assertEquals(empresa.getId(), response.empresaId());
        assertEquals("Analista", response.cargo());
        assertFalse(response.podeGerenciarFuncionarios());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    void deveFalharAoCadastrarFuncionarioSemEmpresaExistente() {
        FuncionarioRepository funcionarioRepository = mock(FuncionarioRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);

        UUID empresaId = UUID.randomUUID();
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.empty());

        FuncionarioService service = new FuncionarioService(funcionarioRepository, empresaRepository);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(new FuncionarioRequest("Carlos", empresaId, "Analista"))
        );

        assertEquals("Empresa inexistente: " + empresaId, ex.getMessage());
    }
}