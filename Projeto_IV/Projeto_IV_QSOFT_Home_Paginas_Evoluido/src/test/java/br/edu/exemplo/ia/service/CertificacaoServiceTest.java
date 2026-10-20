package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Certificacao;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import br.edu.exemplo.ia.dto.CertificacaoRequest;
import br.edu.exemplo.ia.dto.CertificacaoResponse;
import br.edu.exemplo.ia.dto.ResumoCertificacaoResponse;
import br.edu.exemplo.ia.repository.CertificacaoRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes da US PP-19 (Registrar Capacitação e Certificação em IA).
 * Os dois primeiros testes são os cenários do BDD; os demais cobrem as validações.
 * Pontuação usada nos testes: curso = 10 e certificação = 20.
 */
class CertificacaoServiceTest {

    private CertificacaoRepository certificacaoRepository;
    private FuncionarioRepository funcionarioRepository;
    private CertificacaoService service;

    private Empresa empresa;
    private Funcionario funcionario;

    @BeforeEach
    void prepararCenario() {
        certificacaoRepository = mock(CertificacaoRepository.class);
        funcionarioRepository = mock(FuncionarioRepository.class);
        service = new CertificacaoService(certificacaoRepository, funcionarioRepository, new PontuacaoAiLiteracy(10, 20));

        empresa = new Empresa(new EmpresaVO("QSoft"), "Tecnologia");
        funcionario = new Funcionario("Ana Souza", empresa, "Analista");
        when(funcionarioRepository.findById(funcionario.getId())).thenReturn(Optional.of(funcionario));
        when(certificacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Cenário BDD: registro de certificação com sucesso
    @Test
    void deveRegistrarCertificacaoNoPerfilESomarPontosDeAiLiteracy() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "AWS Certified AI Practitioner", "Amazon Web Services", LocalDate.of(2026, 9, 15), "CERTIFICACAO");

        CertificacaoResponse response = service.registrar(request);

        assertNotNull(response.id());
        assertEquals(funcionario.getId(), response.funcionarioId());
        assertEquals("AWS Certified AI Practitioner", response.nome());
        assertEquals("Amazon Web Services", response.instituicao());
        assertEquals(LocalDate.of(2026, 9, 15), response.dataConclusao());
        assertEquals("CERTIFICACAO", response.tipo());
        assertEquals(20, response.pontos());
        assertNotNull(response.dataCriacao());
        verify(certificacaoRepository).save(any(Certificacao.class));
    }

    // Cenário BDD: tentativa de registro sem data de conclusão
    @Test
    void deveExigirDataDeConclusao() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "Fundamentos de IA", "Alura", null, "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Data de conclusão obrigatória", erro.getMessage());
        verify(certificacaoRepository, never()).save(any());
    }

    @Test
    void deveExigirInstituicao() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "Fundamentos de IA", "   ", LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Instituição obrigatória", erro.getMessage());
        verify(certificacaoRepository, never()).save(any());
    }

    @Test
    void deveExigirNomeDoCurso() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "", "Alura", LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Nome do curso/certificação obrigatório", erro.getMessage());
    }

    @Test
    void deveRejeitarNomeOuInstituicaoComMaisDe150Caracteres() {
        String textoLongo = "A".repeat(151);
        CertificacaoRequest nomeLongo = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                textoLongo, "Alura", LocalDate.of(2026, 5, 10), "CURSO");
        CertificacaoRequest instituicaoLonga = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "Fundamentos de IA", textoLongo, LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erroNome = assertThrows(IllegalArgumentException.class, () -> service.registrar(nomeLongo));
        IllegalArgumentException erroInstituicao = assertThrows(IllegalArgumentException.class, () -> service.registrar(instituicaoLonga));

        assertEquals("Nome do curso/certificação deve ter no máximo 150 caracteres", erroNome.getMessage());
        assertEquals("Instituição deve ter no máximo 150 caracteres", erroInstituicao.getMessage());
        verify(certificacaoRepository, never()).save(any());
    }

    @Test
    void deveAceitarNomeComExatamente150Caracteres() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "A".repeat(150), "Alura", LocalDate.of(2026, 5, 10), "CURSO");

        assertEquals(150, service.registrar(request).nome().length());
    }

    @Test
    void deveRejeitarDataDeConclusaoFutura() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "Fundamentos de IA", "Alura", LocalDate.now().plusDays(1), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Data de conclusão não pode ser futura", erro.getMessage());
        verify(certificacaoRepository, never()).save(any());
    }

    @Test
    void deveAceitarDataDeConclusaoDeHoje() {
        LocalDate hoje = LocalDate.now();
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "Fundamentos de IA", "Alura", hoje, "CURSO");

        CertificacaoResponse response = service.registrar(request);

        assertEquals(hoje, response.dataConclusao());
        assertEquals(10, response.pontos());
    }

    @Test
    void deveRejeitarColaboradorInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(funcionarioRepository.findById(idInexistente)).thenReturn(Optional.empty());
        CertificacaoRequest request = new CertificacaoRequest(idInexistente, empresa.getId(),
                "Fundamentos de IA", "Alura", LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Funcionario inexistente: " + idInexistente, erro.getMessage());
        verify(certificacaoRepository, never()).save(any());
    }

    @Test
    void deveExigirColaborador() {
        CertificacaoRequest request = new CertificacaoRequest(null, empresa.getId(),
                "Fundamentos de IA", "Alura", LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Funcionário obrigatório", erro.getMessage());
    }

    @Test
    void deveExigirEmpresa() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), null,
                "Fundamentos de IA", "Alura", LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Empresa obrigatória", erro.getMessage());
    }

    @Test
    void deveRejeitarColaboradorDeOutraEmpresa() {
        UUID outraEmpresaId = UUID.randomUUID();
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), outraEmpresaId,
                "Fundamentos de IA", "Alura", LocalDate.of(2026, 5, 10), "CURSO");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Funcionário não pertence à empresa informada", erro.getMessage());
        verify(certificacaoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarTipoInvalido() {
        CertificacaoRequest request = new CertificacaoRequest(funcionario.getId(), empresa.getId(),
                "Fundamentos de IA", "Alura", LocalDate.of(2026, 5, 10), "PALESTRA");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.registrar(request));

        assertEquals("Tipo deve ser CURSO ou CERTIFICACAO", erro.getMessage());
    }

    @Test
    void deveListarHistoricoDoColaborador() {
        Certificacao curso = new Certificacao(funcionario, "Fundamentos de IA", "Alura",
                LocalDate.of(2026, 3, 1), TipoCertificacao.CURSO);
        Certificacao certificacao = new Certificacao(funcionario, "Azure AI Fundamentals", "Microsoft",
                LocalDate.of(2026, 8, 20), TipoCertificacao.CERTIFICACAO);
        when(certificacaoRepository.findByFuncionarioIdOrderByDataConclusaoDesc(funcionario.getId()))
                .thenReturn(List.of(certificacao, curso));

        List<CertificacaoResponse> historico = service.listar(funcionario.getId());

        assertEquals(2, historico.size());
        assertEquals("Azure AI Fundamentals", historico.get(0).nome());
        assertEquals(20, historico.get(0).pontos());
        assertEquals("Fundamentos de IA", historico.get(1).nome());
        assertEquals(10, historico.get(1).pontos());
    }

    @Test
    void deveRejeitarListagemDeColaboradorInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(funcionarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.listar(idInexistente));
    }

    @Test
    void deveSomarPontosNoResumoDoColaborador() {
        when(certificacaoRepository.findByFuncionarioIdOrderByDataConclusaoDesc(funcionario.getId())).thenReturn(List.of(
                new Certificacao(funcionario, "Fundamentos de IA", "Alura", LocalDate.of(2026, 3, 1), TipoCertificacao.CURSO),
                new Certificacao(funcionario, "Prompt Engineering", "DIO", LocalDate.of(2026, 4, 1), TipoCertificacao.CURSO),
                new Certificacao(funcionario, "Azure AI Fundamentals", "Microsoft", LocalDate.of(2026, 8, 20), TipoCertificacao.CERTIFICACAO)
        ));

        ResumoCertificacaoResponse resumo = service.resumo(funcionario.getId());

        assertEquals(funcionario.getId(), resumo.funcionarioId());
        assertEquals(3, resumo.totalRegistros());
        assertEquals(2, resumo.totalCursos());
        assertEquals(1, resumo.totalCertificacoes());
        assertEquals(40, resumo.pontosAiLiteracy()); // 10 + 10 + 20
    }

    @Test
    void resumoDeColaboradorSemCapacitacoesDeveSerZero() {
        when(certificacaoRepository.findByFuncionarioIdOrderByDataConclusaoDesc(funcionario.getId())).thenReturn(List.of());

        ResumoCertificacaoResponse resumo = service.resumo(funcionario.getId());

        assertEquals(0, resumo.totalRegistros());
        assertEquals(0, resumo.pontosAiLiteracy());
    }
}
