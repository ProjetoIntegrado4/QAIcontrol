package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.dto.EmpresaRequest;
import br.edu.exemplo.ia.dto.EmpresaResponse;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmpresaServiceTest {

    @Test
    void deveCadastrarEmpresaComDadosCompletos() {
        EmpresaRepository repository = mock(EmpresaRepository.class);
        when(repository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EmpresaService service = new EmpresaService(repository);

        EmpresaResponse response = service.create(new EmpresaRequest(
                "QSoft", "QSoft Tecnologia Ltda", "12345678000199", 25,
                "01310-100", "Avenida Paulista, 1000", "São Paulo", "SP",
                "Ana Silva", "Tecnologia"
        ));

        assertEquals("QSoft", response.name());
        assertEquals("QSoft Tecnologia Ltda", response.razaoSocial());
        assertEquals("12345678000199", response.cnpj());
        assertEquals(25, response.numeroFuncionarios());
        assertEquals("01310-100", response.cep());
        assertEquals("Avenida Paulista, 1000", response.endereco());
        assertEquals("São Paulo", response.cidade());
        assertEquals("SP", response.estado());
        assertEquals("Ana Silva", response.adminResponsavel());
        assertEquals("Tecnologia", response.area());
    }
}