package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.domain.entity.AceitePolitica;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.entity.PoliticaUso;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import br.edu.exemplo.ia.integration.alert.AlertClient;
import br.edu.exemplo.ia.integration.alert.AlertResponse;
import br.edu.exemplo.ia.repository.AceitePoliticaRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import br.edu.exemplo.ia.repository.PoliticaUsoRepository;
import br.edu.exemplo.ia.repository.UsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cenário BDD 2 da US PP-21: com política obrigatória publicada, o uso de IA do colaborador que ainda não aceitou a
 * versão vigente é bloqueado. Usa o PoliticaService real (repositórios simulados) junto com o UsageService.
 */
class UsageBloqueioPoliticaTest {

    private UsageRepository usageRepository;
    private EmpresaUseCase empresas;
    private AlertClient alerts;
    private FuncionarioRepository funcionarioRepository;
    private PoliticaService politicas;
    private UsageService service;

    private Empresa empresa;
    private Funcionario funcionario;
    private final List<PoliticaUso> vigentes = new ArrayList<>();
    private final List<AceitePolitica> aceites = new ArrayList<>();

    @BeforeEach
    void prepararCenario() {
        usageRepository = mock(UsageRepository.class);
        empresas = mock(EmpresaUseCase.class);
        alerts = mock(AlertClient.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        PoliticaUsoRepository politicaRepository = mock(PoliticaUsoRepository.class);
        AceitePoliticaRepository aceiteRepository = mock(AceitePoliticaRepository.class);
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        politicas = new PoliticaService(politicaRepository, aceiteRepository, empresaRepository, funcionarioRepository);
        service = new UsageService(usageRepository, empresas, alerts, politicas);

        empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        funcionario = new Funcionario("Ana Souza", empresa, "Analista");
        when(empresas.exists(empresa.getId())).thenReturn(true);
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(funcionarioRepository.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        when(politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresa.getId())).thenReturn(vigentes);
        when(politicaRepository.findById(any())).thenAnswer(invocation -> vigentes.stream()
                .filter(politica -> politica.getId().equals(invocation.getArgument(0))).findFirst());
        when(aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionario.getId())).thenReturn(aceites);
        when(aceiteRepository.findByFuncionarioIdAndPoliticaId(any(), any())).thenReturn(Optional.empty());
        when(aceiteRepository.save(any())).thenAnswer(invocation -> {
            aceites.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(usageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(alerts.evaluate(any(), anyLong(), anyString())).thenReturn(new AlertResponse("INFO", "Consumo dentro do esperado"));
    }

    private UsageRequest usoDoColaborador() {
        return new UsageRequest(empresa.getId(), 500, "gpt-demo", funcionario.getId());
    }

    private PoliticaUso publicarPolitica(boolean obrigatoria) {
        PoliticaUso politica = new PoliticaUso(empresa, "Uso responsável de IA", "Texto", obrigatoria);
        vigentes.add(politica);
        return politica;
    }

    private void aceitar(PoliticaUso politica) {
        politicas.registrarAceite(new AceiteRequest(funcionario.getId(), politica.getId()));
    }

    // Cenário BDD 2: política obrigatória sem aceite bloqueia o registro e informa as pendências
    @Test
    void usoDeIaSemAceiteDaPoliticaObrigatoriaDeveSerBloqueado() {
        PoliticaUso politica = publicarPolitica(true);

        AceitePendenteException erro = assertThrows(AceitePendenteException.class,
                () -> service.register(usoDoColaborador()));

        assertEquals("Aceite da política de uso de IA obrigatório", erro.getMessage());
        assertEquals(1, erro.getPendentes().size());
        assertEquals(politica.getId(), erro.getPendentes().get(0).id());
        verify(usageRepository, never()).save(any());
        verifyNoInteractions(alerts);
    }

    // Cenário BDD 2 (continuação): depois do aceite o uso é registrado normalmente
    @Test
    void usoDeIaDeveSerLiberadoDepoisDoAceite() {
        aceitar(publicarPolitica(true));

        UsageResponse uso = service.register(usoDoColaborador());

        assertEquals(500, uso.tokens());
        assertEquals("INFO", uso.alertLevel());
        verify(usageRepository).save(any(AIUsage.class));
    }

    // Nova versão exige novo aceite: o aceite da versão anterior não libera o uso
    @Test
    void novaVersaoDaPoliticaDeveVoltarABloquearOUso() {
        PoliticaUso v1 = publicarPolitica(true);
        aceitar(v1);
        vigentes.set(0, v1.novaVersao("Uso responsável de IA", "Texto 2", true));

        AceitePendenteException erro = assertThrows(AceitePendenteException.class,
                () -> service.register(usoDoColaborador()));

        assertEquals(2, erro.getPendentes().get(0).versao());
    }

    @Test
    void usoDeIaDeveSerLiberadoDepoisDoAceiteDaNovaVersao() {
        PoliticaUso v1 = publicarPolitica(true);
        aceitar(v1);
        PoliticaUso v2 = v1.novaVersao("Uso responsável de IA", "Texto 2", true);
        vigentes.set(0, v2);
        aceitar(v2);

        UsageResponse uso = service.register(usoDoColaborador());

        assertNotNull(uso.id());
    }

    @Test
    void politicaNaoObrigatoriaNaoDeveBloquearOUso() {
        publicarPolitica(false);

        UsageResponse uso = service.register(usoDoColaborador());

        assertNotNull(uso.id());
    }

    @Test
    void empresaSemPoliticaNaoDeveBloquearOUso() {
        UsageResponse uso = service.register(usoDoColaborador());

        assertNotNull(uso.id());
    }

    // Retrocompatibilidade: sem funcionarioId o registro de uso continua igual ao comportamento atual
    @Test
    void usoSemFuncionarioNaoDeveConsultarPoliticas() {
        publicarPolitica(true);
        PoliticaUseCase politicasSimuladas = mock(PoliticaUseCase.class);
        UsageService semColaborador = new UsageService(usageRepository, empresas, alerts, politicasSimuladas);

        UsageResponse uso = semColaborador.register(new UsageRequest(empresa.getId(), 500, "gpt-demo"));

        assertEquals(empresa.getId(), uso.empresaId());
        assertEquals("INFO", uso.alertLevel());
        verifyNoInteractions(politicasSimuladas);
    }

    @Test
    void requestAntigoDeUsoDeveTerFuncionarioNulo() {
        UsageRequest request = new UsageRequest(empresa.getId(), 500, "gpt-demo");

        assertNull(request.funcionarioId());
    }

    @Test
    void funcionarioDeOutraEmpresaNaoDeveRegistrarUso() {
        Funcionario deOutraEmpresa = new Funcionario("Bruno", new Empresa(new EmpresaVO("Outra"), "Saude"), "Dev");
        when(funcionarioRepository.findById(deOutraEmpresa.getId())).thenReturn(Optional.of(deOutraEmpresa));
        UsageRequest request = new UsageRequest(empresa.getId(), 500, "gpt-demo", deOutraEmpresa.getId());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.register(request));

        assertEquals("Funcionário não pertence à empresa informada", erro.getMessage());
        verify(usageRepository, never()).save(any());
    }

    @Test
    void funcionarioInexistenteNaoDeveRegistrarUso() {
        UsageRequest request = new UsageRequest(empresa.getId(), 500, "gpt-demo", UUID.randomUUID());

        assertThrows(NaoEncontradoException.class, () -> service.register(request));

        verify(usageRepository, never()).save(any());
    }

    @Test
    void empresaInexistenteDeveManterAMensagemDeAntes() {
        UUID inexistente = UUID.randomUUID();
        UsageRequest request = new UsageRequest(inexistente, 500, "gpt-demo", funcionario.getId());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.register(request));

        assertEquals("Empresa inexistente: " + inexistente, erro.getMessage());
    }

    // O construtor antigo (sem PoliticaUseCase) não deixa passar uso com colaborador sem verificar o aceite
    @Test
    void construtorAntigoNaoDeveRegistrarUsoComColaborador() {
        UsageService antigo = new UsageService(usageRepository, empresas, alerts);

        assertThrows(IllegalStateException.class, () -> antigo.register(usoDoColaborador()));

        verify(usageRepository, never()).save(any());
    }
}
