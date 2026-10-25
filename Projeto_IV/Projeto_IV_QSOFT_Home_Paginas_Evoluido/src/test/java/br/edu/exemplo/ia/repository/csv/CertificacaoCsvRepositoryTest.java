package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.config.BaseDadosProperties;
import br.edu.exemplo.ia.domain.entity.Certificacao;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Capacitações na base de dados em CSV (training_records.csv), nos dois sentidos: site -> arquivo e arquivo -> site. */
class CertificacaoCsvRepositoryTest {

    @TempDir
    Path pasta;

    private FuncionarioCsvRepository funcionarios;
    private CertificacaoCsvRepository certificacoes;

    @BeforeEach
    void preparar() throws IOException {
        Files.writeString(pasta.resolve("companies.csv"), """
                company_id,company_name,industry,company_size,employee_count
                C01,NexoCode Solucoes,Tecnologia,Grande,1450
                """);
        Files.writeString(pasta.resolve("employees.csv"), """
                employee_id,company_id,department_id,job_role,seniority,hire_date,cost_center,active
                C01-E0001,C01,C01-D02,Engenheiro(a) DevOps,Pleno,2026-03-01,CC-C01-D02,True
                C01-E0002,C01,C01-D01,Desenvolvedor(a),Junior,2023-12-10,CC-C01-D01,True
                """);
        Files.writeString(pasta.resolve("training_records.csv"), """
                training_id,employee_id,company_id,course_name,course_category,enrollment_date,completion_date,completion_status,score,duration_hours
                TR000001,C01-E0001,C01,IA Responsavel,Governanca,2026-04-06,2026-04-22,Concluido,62.0,1.3
                TR000002,C01-E0001,C01,Prompt Engineering,IA Generativa,2026-05-01,2026-05-20,Concluido,90.0,2.0
                TR000003,C01-E0001,C01,Curso em andamento,IA Generativa,2026-06-01,,Em Andamento,,
                TR000004,C01-E0002,C01,Seguranca,Seguranca e Compliance,2026-04-17,2026-04-27,Concluido,82.0,1.7
                """);
        BaseDadosCsv base = new BaseDadosCsv(new BaseDadosProperties(pasta.toString()));
        EmpresaCsvRepository empresas = new EmpresaCsvRepository(base);
        funcionarios = new FuncionarioCsvRepository(base, empresas, new AdminCsvRepository(base, empresas));
        certificacoes = new CertificacaoCsvRepository(base);
    }

    private Funcionario funcionario(String codigo) {
        return funcionarios.findAll().stream().filter(f -> codigo.equals(f.getCodigo())).findFirst().orElseThrow();
    }

    @Test
    void deveLerOsTreinamentosConcluidosDaBase() {
        Funcionario ana = funcionario("C01-E0001");

        List<Certificacao> lista = certificacoes.findByFuncionarioIdOrderByDataConclusaoDesc(ana.getId());

        assertEquals(List.of("TR000002", "TR000001"), lista.stream().map(Certificacao::getCodigo).toList());
        Certificacao maisRecente = lista.get(0);
        assertEquals("Prompt Engineering", maisRecente.getNome());
        assertEquals(LocalDate.of(2026, 5, 20), maisRecente.getDataConclusao());
        assertEquals(CertificacaoCsvRepository.SEM_INSTITUICAO, maisRecente.getInstituicao());
        assertEquals(TipoCertificacao.CURSO, maisRecente.getTipo());
        assertEquals(ana.getId(), maisRecente.getFuncionarioId());
    }

    @Test
    void deveGravarNovaCertificacaoNoArquivoDeTreinamentos() throws IOException {
        Funcionario ana = funcionario("C01-E0001");

        Certificacao salva = certificacoes.save(new Certificacao(ana, "AWS AI Practitioner", "Amazon Web Services",
                LocalDate.of(2026, 9, 15), TipoCertificacao.CERTIFICACAO));

        assertEquals("TR000005", salva.getCodigo());
        List<String> arquivo = Files.readAllLines(pasta.resolve("training_records.csv"));
        assertTrue(arquivo.get(0).endsWith(",instituicao,tipo,registrado_em"));
        assertTrue(arquivo.get(arquivo.size() - 1).startsWith(
                "TR000005,C01-E0001,C01,AWS AI Practitioner,,2026-09-15,2026-09-15,Concluido,,,Amazon Web Services,CERTIFICACAO,"));

        List<Certificacao> lista = certificacoes.findByFuncionarioIdOrderByDataConclusaoDesc(ana.getId());
        assertEquals(3, lista.size());
        Certificacao lida = lista.get(0);
        assertEquals(salva.getId(), lida.getId());
        assertEquals("Amazon Web Services", lida.getInstituicao());
        assertEquals(TipoCertificacao.CERTIFICACAO, lida.getTipo());
    }

    @Test
    void deveEnxergarTreinamentoAcrescentadoPorForaDoSistema() throws IOException {
        Funcionario bruno = funcionario("C01-E0002");
        assertEquals(1, certificacoes.findByFuncionarioIdOrderByDataConclusaoDesc(bruno.getId()).size());

        Path arquivo = pasta.resolve("training_records.csv");
        Files.writeString(arquivo, "TR000009,C01-E0002,C01,Dados com IA,Dados e Analytics,2026-07-01,2026-07-10,Concluido,70.0,1.0\n",
                StandardOpenOption.APPEND);
        Files.setLastModifiedTime(arquivo, FileTime.fromMillis(System.currentTimeMillis() + 10_000));

        assertEquals("TR000009", certificacoes.findByFuncionarioIdOrderByDataConclusaoDesc(bruno.getId()).get(0).getCodigo());
    }

    @Test
    void deveRecusarFuncionarioForaDaBase() {
        Funcionario semCodigo = new Funcionario("Ana", funcionario("C01-E0001").getEmpresa(), "Analista");
        Certificacao certificacao = new Certificacao(semCodigo, "Curso", "Alura", LocalDate.of(2026, 1, 1),
                TipoCertificacao.CURSO);

        assertThrows(IllegalArgumentException.class, () -> certificacoes.save(certificacao));
    }
}
