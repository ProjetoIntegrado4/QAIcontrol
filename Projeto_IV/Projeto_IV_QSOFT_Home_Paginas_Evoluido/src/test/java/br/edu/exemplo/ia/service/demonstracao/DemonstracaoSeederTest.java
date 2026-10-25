package br.edu.exemplo.ia.service.demonstracao;

import br.edu.exemplo.ia.config.BaseDadosProperties;
import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.domain.entity.AceitePolitica;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.entity.PoliticaUso;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.repository.AceitePoliticaRepository;
import br.edu.exemplo.ia.repository.PoliticaUsoRepository;
import br.edu.exemplo.ia.repository.UsageRepository;
import br.edu.exemplo.ia.repository.csv.AdminCsvRepository;
import br.edu.exemplo.ia.repository.csv.BaseDadosCsv;
import br.edu.exemplo.ia.repository.csv.EmpresaCsvRepository;
import br.edu.exemplo.ia.repository.csv.FuncionarioCsvRepository;
import br.edu.exemplo.ia.service.PoliticaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Carga de demonstração: políticas, aceites e consumo derivados da base de dados em CSV. */
class DemonstracaoSeederTest {

    @TempDir
    Path pasta;

    private EmpresaCsvRepository empresas;
    private FuncionarioCsvRepository funcionarios;
    private PoliticaUsoRepository politicas;
    private AceitePoliticaRepository aceites;
    private UsageRepository usos;
    private PoliticaUseCase politicaUseCase;
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private BaseDadosCsv base;
    private final List<PoliticaUso> publicadas = new ArrayList<>();

    @BeforeEach
    void preparar() throws IOException {
        Files.writeString(pasta.resolve("companies.csv"), """
                company_id,company_name,industry,company_size,employee_count
                C01,NexoCode Solucoes,Tecnologia,Grande,1450
                """);
        // E0001 inativo; E0002 treinado em Governança; E0003..E0008 ativos sem treinamento.
        StringBuilder emp = new StringBuilder("employee_id,company_id,department_id,job_role,seniority,hire_date,cost_center,active\n");
        emp.append("C01-E0001,C01,C01-D01,Dev,Pleno,2024-01-01,CC-C01-D01,False\n");
        for (int i = 2; i <= 8; i++) {
            emp.append("C01-E000").append(i).append(",C01,C01-D01,Dev,Pleno,2024-01-01,CC-C01-D01,True\n");
        }
        Files.writeString(pasta.resolve("employees.csv"), emp);
        Files.writeString(pasta.resolve("training_records.csv"), """
                training_id,employee_id,company_id,course_name,course_category,enrollment_date,completion_date,completion_status,score,duration_hours
                TR000001,C01-E0002,C01,IA Responsavel,Governanca,2026-04-06,2026-04-22,Concluido,62.0,1.3
                TR000002,C01-E0003,C01,IA Responsavel,Governanca,2026-04-06,,Em Andamento,,1.3
                """);
        Files.writeString(pasta.resolve("ai_tools.csv"), """
                ai_tool_id,tool_name,provider,tool_category,licensing_type
                T01,ChatGPT Enterprise,OpenAI,Assistente,Assinatura
                T02,Claude for Work,Anthropic,Assistente,Assinatura
                """);
        Files.writeString(pasta.resolve("approved_ai_tools.csv"), """
                company_id,ai_tool_id,approval_status,approval_date,allowed_departments
                C01,T01,Aprovado,2025-06-22,Todos os departamentos
                C01,T02,Bloqueado,2025-06-22,Nenhum departamento
                """);
        base = new BaseDadosCsv(new BaseDadosProperties(pasta.toString()));
        empresas = new EmpresaCsvRepository(base);
        funcionarios = new FuncionarioCsvRepository(base, empresas, new AdminCsvRepository(base, empresas));
        politicas = mock(PoliticaUsoRepository.class);
        aceites = mock(AceitePoliticaRepository.class);
        usos = mock(UsageRepository.class);
        politicaUseCase = mock(PoliticaUseCase.class);
        jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);

        Empresa empresa = empresas.findAll().get(0);
        when(politicaUseCase.publicar(any())).thenAnswer(i -> publicar(empresa, i.getArgument(0)));
        when(politicaUseCase.publicarNovaVersao(any(), any())).thenAnswer(i -> publicar(empresa, i.getArgument(1)));
        when(politicas.findAllById(any())).thenAnswer(i -> {
            List<UUID> ids = new ArrayList<>();
            ((Iterable<UUID>) i.getArgument(0)).forEach(ids::add);
            return publicadas.stream().filter(p -> ids.contains(p.getId())).toList();
        });
    }

    private PoliticaResponse publicar(Empresa empresa, PoliticaRequest r) {
        PoliticaUso p = new PoliticaUso(empresa, r.titulo(), r.conteudo(), r.obrigatoria());
        publicadas.add(p);
        return new PoliticaResponse(p.getId(), p.getCodigo(), empresa.getId(), empresa.getNome(), p.getTitulo(),
                p.getConteudo(), p.getVersao(), p.isObrigatoria(), true, Instant.now());
    }

    private DemonstracaoSeeder seeder(boolean ativo) {
        return new DemonstracaoSeeder(ativo, empresas, funcionarios, politicas, aceites, usos, politicaUseCase, base, jdbc);
    }

    @SuppressWarnings("unchecked")
    @Test
    void devePublicarPoliticasEAceitesDosColaboradoresAtivos() {
        seeder(true).run(null);

        verify(politicaUseCase, org.mockito.Mockito.times(3)).publicar(any());
        verify(politicaUseCase).publicarNovaVersao(any(), any());
        ArgumentCaptor<List<AceitePolitica>> captor = ArgumentCaptor.forClass(List.class);
        verify(aceites).saveAll(captor.capture());
        List<AceitePolitica> salvos = captor.getValue();

        // Só as duas obrigatórias são aceitas, nunca pelo inativo; o treinado em Governança sempre aceita.
        assertTrue(salvos.stream().allMatch(a -> a.getPolitica().isObrigatoria()));
        assertTrue(salvos.stream().noneMatch(a -> a.getFuncionario().getCodigo().equals("C01-E0001")));
        assertEquals(2, salvos.stream().filter(a -> a.getFuncionario().getCodigo().equals("C01-E0002")).count());
        long colaboradores = salvos.stream().map(a -> a.getFuncionario().getCodigo()).distinct().count();
        assertTrue(colaboradores < 7, "parte dos ativos deve ficar pendente");
    }

    @Test
    void politicaDeFerramentasDeveCitarSoAsAprovadas() {
        Map<String, List<String>> aprovadas = seeder(true).ferramentasAprovadas();
        assertEquals(List.of("ChatGPT Enterprise"), aprovadas.get("C01"));

        List<PoliticaRequest> textos = DemonstracaoSeeder.textos(empresas.findAll().get(0), aprovadas.get("C01"));
        assertTrue(textos.get(2).conteudo().contains("ChatGPT Enterprise"));
        assertTrue(DemonstracaoSeeder.textos(empresas.findAll().get(0), List.of()).get(2).conteudo()
                .contains("nenhuma ferramenta aprovada"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void deveCriarConsumoAPartirDoUsoMaisRecenteDaBase() {
        when(politicas.count()).thenReturn(1L);
        when(jdbc.queryForList(anyString(), anyInt())).thenReturn(List.of(
                Map.of("company_id", "C01", "model_name", "gpt-4o", "tokens", 1500L),
                Map.of("company_id", "C99", "model_name", "gpt-4o", "tokens", 10L)));

        seeder(true).run(null);

        ArgumentCaptor<List<AIUsage>> captor = ArgumentCaptor.forClass(List.class);
        verify(usos).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("gpt-4o", captor.getValue().get(0).getModel());
        assertEquals(1500L, captor.getValue().get(0).getTokens());
        verify(politicaUseCase, never()).publicar(any());
    }

    @Test
    void naoDeveTocarTabelasJaPreenchidasNemRodarDesligado() {
        when(politicas.count()).thenReturn(5L);
        when(usos.count()).thenReturn(5L);
        seeder(true).run(null);
        verify(politicaUseCase, never()).publicar(any());
        verify(usos, never()).saveAll(anyList());

        DemonstracaoSeeder desligado = new DemonstracaoSeeder(false, empresas, funcionarios, politicas, aceites, usos,
                politicaUseCase, base, jdbc);
        desligado.run(null);
        verifyNoInteractions(jdbc);
    }

    @Test
    void falhaNumaParteNaoDeveDerrubarASubida() {
        when(politicas.count()).thenThrow(new IllegalStateException("banco fora do ar"));
        when(jdbc.queryForList(anyString(), anyInt())).thenThrow(new IllegalStateException("sem tabela"));
        seeder(true).run(null);
        verify(usos, never()).saveAll(anyList());
    }
}
