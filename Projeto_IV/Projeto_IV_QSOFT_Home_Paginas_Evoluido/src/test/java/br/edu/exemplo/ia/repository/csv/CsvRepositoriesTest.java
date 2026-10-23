package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.config.BaseDadosProperties;
import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.AdminVO;
import br.edu.exemplo.ia.domain.vo.EmpresaDadosVO;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.domain.vo.FuncionarioVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cadastro na base de dados em CSV, nos dois sentidos: site -> arquivo e arquivo -> site. */
class CsvRepositoriesTest {

    @TempDir
    Path pasta;

    private EmpresaCsvRepository empresas;
    private AdminCsvRepository admins;
    private FuncionarioCsvRepository funcionarios;

    @BeforeEach
    void preparar() throws IOException {
        Files.writeString(pasta.resolve("companies.csv"), """
                company_id,company_name,industry,company_size,employee_count
                C01,NexoCode Solucoes,Tecnologia,Grande,1450
                C02,Vortice Software,Tecnologia,Media,420
                """);
        Files.writeString(pasta.resolve("departments.csv"), """
                department_id,company_id,department_name
                C01-D01,C01,Engenharia de Software
                C01-D02,C01,DevOps e Infraestrutura
                """);
        Files.writeString(pasta.resolve("employees.csv"), """
                employee_id,company_id,department_id,job_role,seniority,hire_date,cost_center,active
                C01-E0001,C01,C01-D02,Engenheiro(a) DevOps,Pleno,2026-03-01,CC-C01-D02,False
                C01-E0002,C01,C01-D01,Desenvolvedor(a),Junior,2023-12-10,CC-C01-D01,True
                C99-E0001,C99,C99-D01,Fantasma,Junior,2023-12-10,CC-C99-D01,True
                """);
        BaseDadosCsv base = new BaseDadosCsv(new BaseDadosProperties(pasta.toString()));
        empresas = new EmpresaCsvRepository(base);
        admins = new AdminCsvRepository(base, empresas);
        funcionarios = new FuncionarioCsvRepository(base, empresas, admins,
                Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC));
    }

    private void acrescentarPorFora(String arquivo, String linha) throws IOException {
        Path p = pasta.resolve(arquivo);
        Files.writeString(p, linha, StandardOpenOption.APPEND);
        Files.setLastModifiedTime(p, FileTime.fromMillis(System.currentTimeMillis() + 10_000));
    }

    @Test
    void deveLerEmpresasDaBase() {
        List<Empresa> lista = empresas.findAll();

        assertEquals(2, lista.size());
        Empresa nexo = lista.get(0);
        assertEquals("C01", nexo.getCodigo());
        assertEquals("NexoCode Solucoes", nexo.getNome());
        assertEquals("Tecnologia", nexo.getArea());
        assertEquals(1450, nexo.getNumeroFuncionarios());
        assertNull(nexo.getCnpj());
        assertTrue(empresas.existsById(nexo.getId()));
        assertEquals(nexo.getId(), empresas.findByCodigo("C01").orElseThrow().getId());
    }

    @Test
    void empresaCadastradaNoSiteVaiParaOCsvComProximoCodigo() throws IOException {
        Empresa nova = new Empresa(new EmpresaVO("QSoft"), new EmpresaDadosVO("Tecnologia", "QSoft Ltda",
                "12345678000199", 25, "01310100", "Av. Paulista, 1000", "São Paulo", "SP", "Ana"));

        empresas.save(nova);

        assertEquals("C03", nova.getCodigo());
        List<String> linhas = Files.readAllLines(pasta.resolve("companies.csv"));
        assertEquals("company_id,company_name,industry,company_size,employee_count,razao_social,cnpj,cep,"
                + "endereco,cidade,estado,admin_responsavel", linhas.get(0));
        assertEquals("C01,NexoCode Solucoes,Tecnologia,Grande,1450,,,,,,,", linhas.get(1));
        assertEquals("C03,QSoft,Tecnologia,Pequena,25,QSoft Ltda,12345678000199,01310100,"
                + "\"Av. Paulista, 1000\",São Paulo,SP,Ana", linhas.get(3));
        Empresa relida = empresas.findById(nova.getId()).orElseThrow();
        assertEquals("Av. Paulista, 1000", relida.getEndereco());
        assertEquals(3, empresas.findAll().size());
    }

    @Test
    void empresaAdicionadaNoCsvApareceNoSite() throws IOException {
        acrescentarPorFora("companies.csv", "C10,Nova Externa,Saude,Media,300\n");

        assertEquals(3, empresas.findAll().size());
        assertEquals("Nova Externa", empresas.findByCodigo("C10").orElseThrow().getNome());
    }

    @Test
    void deveLerFuncionariosComSetorEStatus() {
        List<Funcionario> lista = funcionarios.findAll();

        assertEquals(2, lista.size(), "funcionário de empresa inexistente fica de fora");
        Funcionario primeiro = lista.get(0);
        assertEquals("C01-E0001", primeiro.getCodigo());
        assertEquals("C01-E0001", primeiro.getNome());
        assertEquals("DevOps e Infraestrutura", primeiro.getSetor());
        assertEquals("Engenheiro(a) DevOps", primeiro.getCargo());
        assertEquals("INATIVO", primeiro.getStatusConta());
        assertEquals("ATIVO", lista.get(1).getStatusConta());
        Empresa c01 = empresas.findByCodigo("C01").orElseThrow();
        assertEquals(2, funcionarios.findByEmpresaId(c01.getId()).size());
        assertTrue(funcionarios.findById(primeiro.getId()).isPresent());
    }

    @Test
    void adminEFuncionarioCadastradosNoSiteVaoParaOCsv() throws IOException {
        Empresa c01 = empresas.findByCodigo("C01").orElseThrow();
        Admin admin = admins.save(new Admin(new AdminVO("Ana Lima", "52998224725", "ana@nexo.com", 11999999999L,
                "Gerente", "$2a$hash", "ATIVO", "TOTAL"), c01));
        assertEquals("C01-A001", admin.getCodigo());
        assertEquals("admin_id,company_id,nome,cpf,email,telefone,cargo,senha_hash,status_conta,nivel_acesso",
                Files.readAllLines(pasta.resolve("admins.csv")).get(0));
        assertEquals(1, admins.findByEmpresaId(c01.getId()).size());

        Funcionario existente = funcionarios.save(new Funcionario(FuncionarioVO.completo("Carlos", "52998224725",
                "carlos@nexo.com", 11988887777L, "Analista", "engenharia de software", "M10", "$2a$x", "ATIVO"),
                c01, admins.findById(admin.getId()).orElseThrow()));
        Funcionario novoSetor = funcionarios.save(new Funcionario(FuncionarioVO.completo("Dora", "52998224725",
                "dora@nexo.com", 11988887777L, "Advogada", "Jurídico", "M11", "$2a$y", "INATIVO"), c01, null));

        assertEquals("C01-E0003", existente.getCodigo());
        assertEquals("C01-E0004", novoSetor.getCodigo());
        List<String> linhas = Files.readAllLines(pasta.resolve("employees.csv"));
        assertEquals("C01-E0003,C01,C01-D01,Analista,,2026-10-22,CC-C01-D01,True,Carlos,52998224725,"
                + "carlos@nexo.com,11988887777,M10,$2a$x,C01-A001", linhas.get(4));
        assertTrue(linhas.get(5).startsWith("C01-E0004,C01,C01-D03,Advogada,,2026-10-22,CC-C01-D03,False,Dora"));
        assertTrue(Files.readString(pasta.resolve("departments.csv")).endsWith("C01-D03,C01,Jurídico\n"));

        Funcionario relido = funcionarios.findById(existente.getId()).orElseThrow();
        assertEquals("Carlos", relido.getNome());
        assertEquals("Engenharia de Software", relido.getSetor());
        assertEquals("M10", relido.getMatricula());
        assertEquals(admin.getId(), relido.getGestorResponsavel().getId());
        assertEquals("Jurídico", funcionarios.findById(novoSetor.getId()).orElseThrow().getSetor());
        assertEquals(4, funcionarios.findAll().size());
    }

    @Test
    void adminEFuncionarioAdicionadosNoCsvAparecemNoSite() throws IOException {
        Files.writeString(pasta.resolve("admins.csv"),
                "admin_id,company_id,nome,cpf,email,telefone,cargo,senha_hash,status_conta,nivel_acesso\n"
                        + "C02-A001,C02,Rui,,rui@v.com,11999999999,Diretor,,ATIVO,TOTAL\n");
        acrescentarPorFora("employees.csv", "C02-E0001,C02,,Analista,Junior,2024-01-01,,1\n");

        Admin rui = admins.findAll().get(0);
        assertEquals("Rui", rui.getNome());
        assertEquals("C02", rui.getEmpresa().getCodigo());
        Funcionario novo = funcionarios.findAll().stream()
                .filter(f -> "C02-E0001".equals(f.getCodigo())).findFirst().orElseThrow();
        assertEquals("ATIVO", novo.getStatusConta());
        assertNull(novo.getSetor());
        assertNull(novo.getGestorResponsavel());
    }

    @Test
    void codigosEConversoes() {
        assertEquals("C01-E0010", Codigos.proximo(List.of("C01-E0009", "C02-E0050", "x"), "C01-E", 4));
        assertEquals("C01", Codigos.proximo(List.of(), "C", 2));
        assertEquals(1450L, Codigos.numeroOuNulo("1450.0"));
        assertNull(Codigos.numeroOuNulo("abc"));
        assertNull(Codigos.numeroOuNulo(" "));
        assertEquals("juridico", Codigos.chave("  Jurídico "));
        assertEquals("Pequena", EmpresaCsvRepository.porte(10));
        assertEquals("Media", EmpresaCsvRepository.porte(500));
        assertEquals("Grande", EmpresaCsvRepository.porte(1000));
        assertTrue(FuncionarioCsvRepository.ativo("t"));
        assertFalse(FuncionarioCsvRepository.ativo(null));
        assertEquals("C01", new BaseDadosCsv(new BaseDadosProperties(pasta.toString()))
                .tabela("companies.csv").linhas().get(0).get("company_id"));
    }
}
