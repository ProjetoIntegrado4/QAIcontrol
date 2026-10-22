package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AceitePolitica;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.entity.PoliticaUso;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.AceiteResponse;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.repository.AceitePoliticaRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import br.edu.exemplo.ia.repository.PoliticaUsoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes da US PP-21 (Registrar Política de Uso Responsável de IA - LGPD).
 * Cada teste indica o cenário BDD ou o critério de aceite (CA) do documento do projeto que ele cobre.
 */
class PoliticaServiceTest {

    private PoliticaUsoRepository politicaRepository;
    private AceitePoliticaRepository aceiteRepository;
    private EmpresaRepository empresaRepository;
    private FuncionarioRepository funcionarioRepository;
    private PoliticaService service;

    private Empresa empresa;
    private Funcionario funcionario;

    @BeforeEach
    void prepararCenario() {
        politicaRepository = mock(PoliticaUsoRepository.class);
        aceiteRepository = mock(AceitePoliticaRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        service = new PoliticaService(politicaRepository, aceiteRepository, empresaRepository, funcionarioRepository);

        empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        funcionario = new Funcionario("Ana Souza", empresa, "Analista");
        when(empresaRepository.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(funcionarioRepository.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        when(politicaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aceiteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aceiteRepository.findByFuncionarioIdAndPoliticaId(any(), any())).thenReturn(Optional.empty());
    }

    private PoliticaRequest pedido(String titulo, String conteudo, boolean obrigatoria) {
        return new PoliticaRequest(empresa.getId(), titulo, conteudo, obrigatoria);
    }

    private PoliticaUso politicaCadastrada(String titulo, boolean obrigatoria) {
        PoliticaUso politica = new PoliticaUso(empresa, titulo, "Texto", obrigatoria);
        when(politicaRepository.findById(politica.getId())).thenReturn(Optional.of(politica));
        return politica;
    }

    // Cenário BDD 1 + CA-14.2: o admin publica e a política fica visível para os colaboradores da empresa
    @Test
    void devePublicarPoliticaEDeixarVisivelParaOsColaboradoresDaEmpresa() {
        PoliticaResponse publicada = service.publicar(pedido("Uso responsável de IA",
                "Não insira dados pessoais de clientes em ferramentas de IA.", true));
        ArgumentCaptor<PoliticaUso> salva = ArgumentCaptor.forClass(PoliticaUso.class);
        verify(politicaRepository).save(salva.capture());
        when(politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresa.getId()))
                .thenReturn(List.of(salva.getValue()));

        List<PoliticaResponse> visiveis = service.listarVigentes(empresa.getId());

        assertNotNull(publicada.id());
        assertNotNull(publicada.codigo());
        assertEquals(empresa.getId(), publicada.empresaId());
        assertEquals("QSoft", publicada.empresaNome());
        assertEquals("Uso responsável de IA", publicada.titulo());
        assertEquals(1, publicada.versao());
        assertTrue(publicada.obrigatoria());
        assertTrue(publicada.vigente());
        assertNotNull(publicada.publicadaEm());
        assertEquals(1, visiveis.size());
        assertEquals(publicada.id(), visiveis.get(0).id());
    }

    @Test
    void deveRejeitarPublicacaoEmEmpresaInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(empresaRepository.findById(inexistente)).thenReturn(Optional.empty());

        NaoEncontradoException erro = assertThrows(NaoEncontradoException.class,
                () -> service.publicar(new PoliticaRequest(inexistente, "Uso de IA", "Texto", true)));

        assertEquals("Empresa inexistente: " + inexistente, erro.getMessage());
        verify(politicaRepository, never()).save(any());
    }

    @Test
    void deveExigirEmpresaNaPublicacao() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.publicar(new PoliticaRequest(null, "Uso de IA", "Texto", true)));

        assertEquals("Empresa obrigatória", erro.getMessage());
    }

    @Test
    void deveExigirTituloNaPublicacao() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.publicar(pedido("   ", "Texto", true)));

        assertEquals("Título da política obrigatório", erro.getMessage());
        verify(politicaRepository, never()).save(any());
    }

    @Test
    void deveExigirConteudoNaPublicacao() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.publicar(pedido("Uso de IA", "", true)));

        assertEquals("Conteúdo da política obrigatório", erro.getMessage());
        verify(politicaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarListagemDeEmpresaInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(empresaRepository.findById(inexistente)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.listarVigentes(inexistente));
    }

    // CA-14.3 / RN14: alteração gera nova versão sem excluir a anterior; só a nova fica vigente
    @Test
    void alteracaoDeveGerarNovaVersaoMantendoAAnteriorNoHistorico() {
        PoliticaUso v1 = new PoliticaUso(empresa, "Uso de IA", "Texto 1", false);
        when(politicaRepository.findByCodigoOrderByVersaoDesc(v1.getCodigo())).thenReturn(List.of(v1));

        PoliticaResponse v2 = service.publicarNovaVersao(v1.getCodigo(), pedido("Uso de IA", "Texto 2", true));

        assertEquals(v1.getCodigo(), v2.codigo());
        assertEquals(2, v2.versao());
        assertTrue(v2.vigente());
        assertTrue(v2.obrigatoria());
        assertFalse(v1.isVigente());
        assertEquals("Texto 1", v1.getConteudo());
        ArgumentCaptor<PoliticaUso> salvas = ArgumentCaptor.forClass(PoliticaUso.class);
        verify(politicaRepository, times(2)).save(salvas.capture());
        assertSame(v1, salvas.getAllValues().get(0));
        assertEquals(2, salvas.getAllValues().get(1).getVersao());
        verify(politicaRepository, never()).delete(any());
    }

    @Test
    void novaVersaoPodeOmitirAEmpresa() {
        PoliticaUso v1 = new PoliticaUso(empresa, "Uso de IA", "Texto 1", true);
        when(politicaRepository.findByCodigoOrderByVersaoDesc(v1.getCodigo())).thenReturn(List.of(v1));

        PoliticaResponse v2 = service.publicarNovaVersao(v1.getCodigo(),
                new PoliticaRequest(null, "Uso de IA", "Texto 2", true));

        assertEquals(empresa.getId(), v2.empresaId());
    }

    @Test
    void novaVersaoNaoDeveTrocarAEmpresaDaPolitica() {
        PoliticaUso v1 = new PoliticaUso(empresa, "Uso de IA", "Texto 1", true);
        when(politicaRepository.findByCodigoOrderByVersaoDesc(v1.getCodigo())).thenReturn(List.of(v1));
        PoliticaRequest outraEmpresa = new PoliticaRequest(UUID.randomUUID(), "Uso de IA", "Texto 2", true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.publicarNovaVersao(v1.getCodigo(), outraEmpresa));

        assertEquals("A empresa de uma política não pode ser alterada", erro.getMessage());
        assertTrue(v1.isVigente());
        verify(politicaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarNovaVersaoDePoliticaInexistente() {
        UUID codigo = UUID.randomUUID();
        when(politicaRepository.findByCodigoOrderByVersaoDesc(codigo)).thenReturn(List.of());

        NaoEncontradoException erro = assertThrows(NaoEncontradoException.class,
                () -> service.publicarNovaVersao(codigo, pedido("Uso de IA", "Texto", true)));

        assertEquals("Política inexistente: " + codigo, erro.getMessage());
    }

    @Test
    void deveListarHistoricoDeVersoes() {
        PoliticaUso v1 = new PoliticaUso(empresa, "Uso de IA", "Texto 1", true);
        PoliticaUso v2 = v1.novaVersao("Uso de IA", "Texto 2", true);
        when(politicaRepository.findByCodigoOrderByVersaoDesc(v1.getCodigo())).thenReturn(List.of(v2, v1));

        List<PoliticaResponse> versoes = service.listarVersoes(v1.getCodigo());

        assertEquals(2, versoes.size());
        assertEquals(2, versoes.get(0).versao());
        assertTrue(versoes.get(0).vigente());
        assertEquals(1, versoes.get(1).versao());
        assertFalse(versoes.get(1).vigente());
    }

    @Test
    void deveRejeitarHistoricoDePoliticaInexistente() {
        UUID codigo = UUID.randomUUID();
        when(politicaRepository.findByCodigoOrderByVersaoDesc(codigo)).thenReturn(List.of());

        assertThrows(NaoEncontradoException.class, () -> service.listarVersoes(codigo));
    }

    // CA-15.1: o colaborador vê a política vigente pendente; política não obrigatória e já aceita não aparecem
    @Test
    void pendentesDevemTrazerSoAsObrigatoriasVigentesAindaNaoAceitas() {
        PoliticaUso obrigatoria = new PoliticaUso(empresa, "Uso de IA", "Texto", true);
        PoliticaUso opcional = new PoliticaUso(empresa, "Boas práticas", "Texto", false);
        PoliticaUso jaAceita = new PoliticaUso(empresa, "Dados pessoais", "Texto", true);
        when(politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresa.getId()))
                .thenReturn(List.of(opcional, jaAceita, obrigatoria));
        when(aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionario.getId()))
                .thenReturn(List.of(new AceitePolitica(funcionario, jaAceita)));

        List<PoliticaResponse> pendentes = service.listarPendentes(empresa.getId(), funcionario.getId());

        assertEquals(1, pendentes.size());
        assertEquals(obrigatoria.getId(), pendentes.get(0).id());
        assertEquals("Texto", pendentes.get(0).conteudo());
    }

    @Test
    void deveRejeitarPendentesDeFuncionarioDeOutraEmpresa() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.listarPendentes(UUID.randomUUID(), funcionario.getId()));

        assertEquals("Funcionário não pertence à empresa informada", erro.getMessage());
    }

    @Test
    void deveExigirEmpresaNasPendentes() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.listarPendentes(null, funcionario.getId()));

        assertEquals("Empresa obrigatória", erro.getMessage());
    }

    @Test
    void deveExigirFuncionarioNasPendentes() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.listarPendentes(empresa.getId(), null));

        assertEquals("Funcionário obrigatório", erro.getMessage());
    }

    // CA-15.2 e CA-15.3 / RN15: o aceite registra usuário, versão e data
    @Test
    void deveRegistrarAceiteComFuncionarioVersaoEData() {
        PoliticaUso politica = politicaCadastrada("Uso de IA", true);

        AceiteResponse aceite = service.registrarAceite(new AceiteRequest(funcionario.getId(), politica.getId()));

        assertNotNull(aceite.id());
        assertEquals(funcionario.getId(), aceite.funcionarioId());
        assertEquals("Ana Souza", aceite.funcionarioNome());
        assertEquals(politica.getId(), aceite.politicaId());
        assertEquals(politica.getCodigo(), aceite.codigo());
        assertEquals("Uso de IA", aceite.titulo());
        assertEquals(1, aceite.versao());
        assertNotNull(aceite.dataAceite());
        verify(aceiteRepository).save(any(AceitePolitica.class));
    }

    // Decisão documentada: aceitar de novo a mesma versão devolve o aceite existente (não duplica nem muda a data)
    @Test
    void aceiteDuplicadoDaMesmaVersaoDevolveOAceiteExistente() {
        PoliticaUso politica = politicaCadastrada("Uso de IA", true);
        AceitePolitica existente = new AceitePolitica(funcionario, politica);
        when(aceiteRepository.findByFuncionarioIdAndPoliticaId(funcionario.getId(), politica.getId()))
                .thenReturn(Optional.of(existente));

        AceiteResponse aceite = service.registrarAceite(new AceiteRequest(funcionario.getId(), politica.getId()));

        assertEquals(existente.getId(), aceite.id());
        assertEquals(existente.getDataAceite(), aceite.dataAceite());
        verify(aceiteRepository, never()).save(any());
    }

    @Test
    void aceiteSimultaneoBarradoPeloBancoDevolveOAceiteGravado() {
        PoliticaUso politica = politicaCadastrada("Uso de IA", true);
        AceitePolitica gravadoPorOutraRequisicao = new AceitePolitica(funcionario, politica);
        when(aceiteRepository.findByFuncionarioIdAndPoliticaId(funcionario.getId(), politica.getId()))
                .thenReturn(Optional.empty(), Optional.of(gravadoPorOutraRequisicao));
        when(aceiteRepository.save(any())).thenThrow(new DataIntegrityViolationException("uk_aceite_funcionario_politica"));

        AceiteResponse aceite = service.registrarAceite(new AceiteRequest(funcionario.getId(), politica.getId()));

        assertEquals(gravadoPorOutraRequisicao.getId(), aceite.id());
    }

    @Test
    void deveRejeitarAceiteDeFuncionarioDeOutraEmpresa() {
        Empresa outra = new Empresa(new EmpresaVO("Outra"), "Saude");
        PoliticaUso politicaDeOutraEmpresa = new PoliticaUso(outra, "Uso de IA", "Texto", true);
        when(politicaRepository.findById(politicaDeOutraEmpresa.getId())).thenReturn(Optional.of(politicaDeOutraEmpresa));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAceite(new AceiteRequest(funcionario.getId(), politicaDeOutraEmpresa.getId())));

        assertEquals("Funcionário não pertence à empresa da política", erro.getMessage());
        verify(aceiteRepository, never()).save(any());
    }

    @Test
    void deveRejeitarAceiteDeVersaoQueNaoEstaMaisVigente() {
        PoliticaUso v1 = politicaCadastrada("Uso de IA", true);
        v1.novaVersao("Uso de IA", "Texto 2", true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAceite(new AceiteRequest(funcionario.getId(), v1.getId())));

        assertEquals("Só é possível aceitar a versão vigente da política", erro.getMessage());
    }

    @Test
    void deveRejeitarAceiteDePoliticaInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(politicaRepository.findById(inexistente)).thenReturn(Optional.empty());

        NaoEncontradoException erro = assertThrows(NaoEncontradoException.class,
                () -> service.registrarAceite(new AceiteRequest(funcionario.getId(), inexistente)));

        assertEquals("Política inexistente: " + inexistente, erro.getMessage());
    }

    @Test
    void deveRejeitarAceiteDeFuncionarioInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(funcionarioRepository.findById(inexistente)).thenReturn(Optional.empty());

        NaoEncontradoException erro = assertThrows(NaoEncontradoException.class,
                () -> service.registrarAceite(new AceiteRequest(inexistente, UUID.randomUUID())));

        assertEquals("Funcionário inexistente: " + inexistente, erro.getMessage());
    }

    @Test
    void deveExigirPoliticaNoAceite() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAceite(new AceiteRequest(funcionario.getId(), null)));

        assertEquals("Política obrigatória", erro.getMessage());
    }

    // Nova versão exige novo aceite: o aceite da v1 não vale para a v2
    @Test
    void novaVersaoDeveFicarPendenteMesmoComAceiteDaAnterior() {
        PoliticaUso v1 = new PoliticaUso(empresa, "Uso de IA", "Texto 1", true);
        AceitePolitica aceiteV1 = new AceitePolitica(funcionario, v1);
        PoliticaUso v2 = v1.novaVersao("Uso de IA", "Texto 2", true);
        when(politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresa.getId())).thenReturn(List.of(v2));
        when(aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionario.getId())).thenReturn(List.of(aceiteV1));

        List<PoliticaResponse> pendentes = service.listarPendentes(empresa.getId(), funcionario.getId());

        assertEquals(1, pendentes.size());
        assertEquals(2, pendentes.get(0).versao());
    }

    // CA-15.4: o histórico de aceites continua disponível depois de uma nova versão
    @Test
    void historicoDeAceitesDeveManterAVersaoAnteriorDepoisDeNovaVersao() {
        PoliticaUso v1 = new PoliticaUso(empresa, "Uso de IA", "Texto 1", true);
        AceitePolitica aceiteV1 = new AceitePolitica(funcionario, v1);
        v1.novaVersao("Uso de IA", "Texto 2", true);
        when(aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionario.getId())).thenReturn(List.of(aceiteV1));

        List<AceiteResponse> historico = service.listarAceites(funcionario.getId());

        assertEquals(1, historico.size());
        assertEquals(1, historico.get(0).versao());
        assertEquals(v1.getId(), historico.get(0).politicaId());
        assertNotNull(historico.get(0).dataAceite());
    }

    @Test
    void deveRejeitarHistoricoDeAceitesDeFuncionarioInexistente() {
        UUID inexistente = UUID.randomUUID();
        when(funcionarioRepository.findById(inexistente)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.listarAceites(inexistente));
    }
}
