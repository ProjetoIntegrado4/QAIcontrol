package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
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

        Empresa empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(funcionarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FuncionarioService service = new FuncionarioService(funcionarioRepository, empresaRepository);
        FuncionarioResponse response = service.create(new FuncionarioRequest(
            "Carlos Silva", "529.982.247-25", "carlos@qsoft.com", 11999990000L, "Analista",
            "Tecnologia", empresa.getId(), "Q001", "senha-segura", "ATIVO", null
        ));

        assertEquals("Carlos Silva", response.nome());
        assertEquals("52998224725", response.cpf());
        assertEquals("carlos@qsoft.com", response.emailCorporativo());
        assertEquals(11999990000L, response.telefone());
        assertEquals("Tecnologia", response.setor());
        assertEquals("Q001", response.matricula());
        assertEquals("ATIVO", response.statusConta());
        assertEquals(null, response.gestorResponsavelId());
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
            () -> service.create(new FuncionarioRequest(
                "Carlos Silva", "52998224725", "carlos@qsoft.com", 11999990000L, "Analista",
                "Tecnologia", empresaId, "Q001", "senha-segura", "ATIVO", null
            ))
        );

        assertEquals("Empresa inexistente: " + empresaId, ex.getMessage());
    }

        @Test
        void deveAssociarGestorDaMesmaEmpresa() {
        FuncionarioRepository funcionarioRepository = mock(FuncionarioRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        Empresa empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        Funcionario gestor = new Funcionario(
            "Ana Gestora", "52998224725", "ana@qsoft.com", 11999990001L, "Gerente",
            "Tecnologia", empresa, "Q002", "senha-gestora", "ATIVO", null
        );
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(funcionarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
        when(funcionarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FuncionarioService service = new FuncionarioService(funcionarioRepository, empresaRepository);
        FuncionarioResponse response = service.create(new FuncionarioRequest(
            "Carlos Silva", "52998224725", "carlos@qsoft.com", 11999990000L, "Analista",
            "Tecnologia", empresa.getId(), "Q001", "senha-segura", "ATIVO", gestor.getId()
        ));

        assertEquals(gestor.getId(), response.gestorResponsavelId());
        assertEquals("Ana Gestora", response.gestorResponsavelNome());
        }

        @Test
        void deveRejeitarGestorDeOutraEmpresa() {
        FuncionarioRepository funcionarioRepository = mock(FuncionarioRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        Empresa empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        Empresa outraEmpresa = new Empresa(new EmpresaVO("Outra"), "Servicos");
        Funcionario gestor = new Funcionario(
            "Ana Gestora", "52998224725", "ana@outra.com", 11999990001L, "Gerente",
            "Tecnologia", outraEmpresa, "Q002", "senha-gestora", "ATIVO", null
        );
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(funcionarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

        FuncionarioService service = new FuncionarioService(funcionarioRepository, empresaRepository);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.create(new FuncionarioRequest(
                "Carlos Silva", "52998224725", "carlos@qsoft.com", 11999990000L, "Analista",
                "Tecnologia", empresa.getId(), "Q001", "senha-segura", "ATIVO", gestor.getId()
            ))
        );

        assertEquals("Gestor responsavel deve pertencer a mesma empresa", exception.getMessage());
        }
}