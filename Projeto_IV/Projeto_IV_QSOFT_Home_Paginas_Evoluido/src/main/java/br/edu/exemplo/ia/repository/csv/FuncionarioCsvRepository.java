package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.FuncionarioVO;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Funcionários na base de dados em CSV (employees.csv), com o setor ligado a departments.csv.
 *
 * <p>Colunas da base: employee_id, company_id, department_id, job_role, seniority, hire_date, cost_center, active.
 * Os dados que só o site tem (nome, CPF, e-mail...) ficam em colunas extras no fim do arquivo. O setor
 * informado no site vira o department_id do departamento com esse nome na empresa; se não existir, o
 * departamento é criado em departments.csv. A senha fica só como hash BCrypt.
 */
@Repository
public class FuncionarioCsvRepository implements FuncionarioRepository {

    private final TabelaCsv tabela;
    private final TabelaCsv departamentos;
    private final EmpresaCsvRepository empresas;
    private final AdminCsvRepository admins;
    private final Clock relogio;
    private Snapshot snapshot;

    @Autowired
    public FuncionarioCsvRepository(BaseDadosCsv base, EmpresaCsvRepository empresas, AdminCsvRepository admins) {
        this(base, empresas, admins, Clock.systemDefaultZone());
    }

    FuncionarioCsvRepository(BaseDadosCsv base, EmpresaCsvRepository empresas, AdminCsvRepository admins,
                             Clock relogio) {
        this.tabela = base.tabela(BaseDadosCsv.FUNCIONARIOS);
        this.departamentos = base.tabela(BaseDadosCsv.DEPARTAMENTOS);
        this.empresas = empresas;
        this.admins = admins;
        this.relogio = relogio;
    }

    @Override
    public synchronized List<Funcionario> findAll() {
        return atual().funcionarios;
    }

    @Override
    public synchronized Optional<Funcionario> findById(UUID id) {
        return Optional.ofNullable(atual().porId.get(id));
    }

    @Override
    public synchronized Funcionario save(Funcionario funcionario) {
        Empresa empresa = funcionario.getEmpresa();
        String codigoEmpresa = empresa.getCodigo();
        List<String> codigos = tabela.linhas().stream().map(l -> l.get("employee_id")).toList();
        funcionario.definirCodigo(Codigos.proximo(codigos, codigoEmpresa + "-E", 4));
        String departamento = departamento(codigoEmpresa, funcionario.getSetor());
        Admin gestor = funcionario.getGestorResponsavel();

        Map<String, String> linha = new LinkedHashMap<>();
        linha.put("employee_id", funcionario.getCodigo());
        linha.put("company_id", codigoEmpresa);
        linha.put("department_id", departamento);
        linha.put("job_role", funcionario.getCargo());
        linha.put("seniority", "");
        linha.put("hire_date", LocalDate.now(relogio).toString());
        linha.put("cost_center", departamento.isEmpty() ? "" : "CC-" + departamento);
        linha.put("active", "ATIVO".equals(funcionario.getStatusConta()) ? "True" : "False");
        linha.put("nome", funcionario.getNome());
        linha.put("cpf", Codigos.texto(funcionario.getCpf()));
        linha.put("email_corporativo", Codigos.texto(funcionario.getEmailCorporativo()));
        linha.put("telefone", Codigos.texto(funcionario.getTelefone()));
        linha.put("matricula", Codigos.texto(funcionario.getMatricula()));
        linha.put("senha_hash", Codigos.texto(funcionario.getSenha()));
        linha.put("gestor_admin_id", gestor == null ? "" : Codigos.texto(gestor.getCodigo()));
        tabela.adicionar(linha);
        snapshot = null;
        return funcionario;
    }

    /** department_id do setor na empresa; cria o departamento em departments.csv se ele não existir. */
    private String departamento(String empresa, String setor) {
        if (setor == null || setor.isBlank()) {
            return "";
        }
        List<Map<String, String>> daEmpresa = departamentos.linhas().stream()
                .filter(l -> empresa.equals(l.get("company_id")))
                .toList();
        for (Map<String, String> l : daEmpresa) {
            if (Codigos.chave(l.get("department_name")).equals(Codigos.chave(setor))) {
                return l.get("department_id");
            }
        }
        List<String> codigos = departamentos.linhas().stream().map(l -> l.get("department_id")).toList();
        String codigo = Codigos.proximo(codigos, empresa + "-D", 2);
        Map<String, String> linha = new LinkedHashMap<>();
        linha.put("department_id", codigo);
        linha.put("company_id", empresa);
        linha.put("department_name", setor.trim());
        departamentos.adicionar(linha);
        return codigo;
    }

    private Snapshot atual() {
        String versao = tabela.versao() + "|" + departamentos.versao() + "|" + admins.versao();
        if (snapshot == null || !snapshot.versao.equals(versao)) {
            Map<String, String> nomesDepartamentos = departamentos.linhas().stream()
                    .filter(l -> Codigos.textoOuNulo(l.get("department_id")) != null)
                    .collect(Collectors.toMap(l -> l.get("department_id"), l -> l.get("department_name"), (a, b) -> a));
            List<Funcionario> funcionarios = new ArrayList<>();
            for (Map<String, String> l : tabela.linhas()) {
                String codigo = Codigos.textoOuNulo(l.get("employee_id"));
                Optional<Empresa> empresa = empresas.findByCodigo(l.get("company_id"));
                if (codigo != null && empresa.isPresent()) {
                    funcionarios.add(funcionario(codigo, l, empresa.get(), nomesDepartamentos));
                }
            }
            snapshot = new Snapshot(versao, List.copyOf(funcionarios), funcionarios.stream()
                    .collect(Collectors.toMap(Funcionario::getId, Function.identity(), (a, b) -> a)));
        }
        return snapshot;
    }

    private Funcionario funcionario(String codigo, Map<String, String> l, Empresa empresa,
                                    Map<String, String> nomesDepartamentos) {
        String departamento = Codigos.textoOuNulo(l.get("department_id"));
        String setor = departamento == null ? null : nomesDepartamentos.getOrDefault(departamento, departamento);
        String nome = Codigos.textoOuNulo(l.get("nome"));
        String matricula = Codigos.textoOuNulo(l.get("matricula"));
        Admin gestor = admins.findByCodigo(l.get("gestor_admin_id"))
                .filter(a -> a.getEmpresa().getId().equals(empresa.getId()))
                .orElse(null);
        return Funcionario.restaurar(codigo, new FuncionarioVO(
                nome == null ? codigo : nome,
                Codigos.textoOuNulo(l.get("cpf")),
                Codigos.textoOuNulo(l.get("email_corporativo")),
                Codigos.numeroOuNulo(l.get("telefone")),
                Codigos.textoOuNulo(l.get("job_role")),
                setor,
                matricula == null ? codigo : matricula,
                Codigos.textoOuNulo(l.get("senha_hash")),
                ativo(l.get("active")) ? "ATIVO" : "INATIVO"
        ), empresa, gestor);
    }

    static boolean ativo(String valor) {
        String v = valor == null ? "" : valor.trim().toLowerCase();
        return v.equals("true") || v.equals("t") || v.equals("1") || v.equals("ativo") || v.equals("sim");
    }

    private record Snapshot(String versao, List<Funcionario> funcionarios, Map<UUID, Funcionario> porId) {
    }
}
