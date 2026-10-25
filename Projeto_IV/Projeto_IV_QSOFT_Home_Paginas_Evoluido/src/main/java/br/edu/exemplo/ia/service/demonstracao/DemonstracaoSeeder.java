package br.edu.exemplo.ia.service.demonstracao;

import br.edu.exemplo.ia.domain.entity.AIUsage;
import br.edu.exemplo.ia.domain.entity.AceitePolitica;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.entity.PoliticaUso;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.repository.AceitePoliticaRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import br.edu.exemplo.ia.repository.PoliticaUsoRepository;
import br.edu.exemplo.ia.repository.UsageRepository;
import br.edu.exemplo.ia.repository.csv.BaseDadosCsv;
import br.edu.exemplo.ia.service.PoliticaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Carga de demonstração das tabelas que só existem no PostgreSQL e começam vazias: políticas de uso de IA,
 * aceites dos colaboradores (US PP-21) e consumo de IA. Tudo é derivado da base de dados em CSV:
 *
 * <ul>
 *   <li>cada empresa recebe três políticas (uma delas já na versão 2), citando as ferramentas aprovadas
 *       para ela em approved_ai_tools.csv;</li>
 *   <li>aceitam as políticas obrigatórias os colaboradores ativos que concluíram treinamento de Governança ou
 *       de Segurança e Compliance (training_records.csv) e cerca de dois terços dos demais ativos; o restante fica
 *       pendente, para a tela de Governança e o bloqueio de consumo terem o que mostrar;</li>
 *   <li>o consumo recebe os usos mais recentes de cada empresa da tabela ai_usage_logs (importada dos CSVs).</li>
 * </ul>
 *
 * <p>Roda depois da importação dos CSVs e só preenche tabela vazia, então nunca duplica nem apaga nada.
 * Desligue com QSOFT_DEMONSTRACAO=false.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemonstracaoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemonstracaoSeeder.class);
    static final int USOS_POR_EMPRESA = 15;
    static final Set<String> CATEGORIAS_GOVERNANCA = Set.of("governanca", "seguranca e compliance");

    private final boolean ativo;
    private final EmpresaRepository empresas;
    private final FuncionarioRepository funcionarios;
    private final PoliticaUsoRepository politicas;
    private final AceitePoliticaRepository aceites;
    private final UsageRepository usos;
    private final PoliticaUseCase politicaUseCase;
    private final BaseDadosCsv base;
    private final JdbcTemplate jdbc;

    public DemonstracaoSeeder(@Value("${qsoft.demonstracao.popular:true}") boolean ativo,
                              EmpresaRepository empresas, FuncionarioRepository funcionarios,
                              PoliticaUsoRepository politicas, AceitePoliticaRepository aceites,
                              UsageRepository usos, PoliticaUseCase politicaUseCase, BaseDadosCsv base,
                              JdbcTemplate jdbc) {
        this.ativo = ativo;
        this.empresas = empresas;
        this.funcionarios = funcionarios;
        this.politicas = politicas;
        this.aceites = aceites;
        this.usos = usos;
        this.politicaUseCase = politicaUseCase;
        this.base = base;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ativo) {
            return;
        }
        executar("políticas e aceites", this::popularPoliticas);
        executar("consumo de IA", this::popularConsumo);
    }

    private void executar(String parte, Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            log.warn("Demonstração: {} não foram populados: {}", parte, e.getMessage());
        }
    }

    void popularPoliticas() {
        if (politicas.count() > 0) {
            return;
        }
        Map<String, List<String>> aprovadas = ferramentasAprovadas();
        Set<String> treinados = treinadosEmGovernanca();
        int totalPoliticas = 0;
        int totalAceites = 0;
        for (Empresa empresa : empresas.findAll()) {
            List<UUID> obrigatorias = new ArrayList<>();
            for (PoliticaRequest p : textos(empresa, aprovadas.getOrDefault(empresa.getCodigo(), List.of()))) {
                PoliticaResponse publicada = politicaUseCase.publicar(p);
                totalPoliticas++;
                if (p.titulo().startsWith("Uso responsável")) {
                    // Revisão já publicada, para a tela mostrar o histórico de versões.
                    publicada = politicaUseCase.publicarNovaVersao(publicada.codigo(), new PoliticaRequest(
                            empresa.getId(), p.titulo(), p.conteudo() + "\n\nRevisão 2: incluída a obrigação de "
                            + "revisar por uma pessoa todo conteúdo gerado por IA antes de enviá-lo a clientes.", true));
                    totalPoliticas++;
                }
                if (publicada.obrigatoria()) {
                    obrigatorias.add(publicada.id());
                }
            }
            List<PoliticaUso> vigentes = politicas.findAllById(obrigatorias);
            List<AceitePolitica> novos = new ArrayList<>();
            for (Funcionario f : funcionarios.findByEmpresaId(empresa.getId())) {
                if (!"ATIVO".equals(f.getStatusConta())) {
                    continue;
                }
                boolean aceita = treinados.contains(f.getCodigo()) || Math.floorMod(f.getCodigo().hashCode(), 3) != 0;
                if (aceita) {
                    vigentes.forEach(p -> novos.add(new AceitePolitica(f, p)));
                }
            }
            aceites.saveAll(novos);
            totalAceites += novos.size();
        }
        log.info("Demonstração: {} versões de políticas e {} aceites criados.", totalPoliticas, totalAceites);
    }

    void popularConsumo() {
        if (usos.count() > 0) {
            return;
        }
        Map<String, UUID> idEmpresa = empresas.findAll().stream()
                .collect(Collectors.toMap(Empresa::getCodigo, Empresa::getId, (a, b) -> a));
        List<Map<String, Object>> linhas = jdbc.queryForList("""
                select company_id, model_name, tokens from (
                    select u.company_id, m.model_name,
                           coalesce(u.input_tokens, 0) + coalesce(u.output_tokens, 0) as tokens,
                           row_number() over (partition by u.company_id order by u."timestamp" desc) as n
                    from ai_usage_logs u join ai_models m on m.model_id = u.model_id
                    where u.request_status = 'success'
                ) x where n <= ? and tokens > 0 order by company_id""", USOS_POR_EMPRESA);
        List<AIUsage> novos = new ArrayList<>();
        for (Map<String, Object> l : linhas) {
            UUID empresa = idEmpresa.get(String.valueOf(l.get("company_id")));
            if (empresa != null) {
                novos.add(new AIUsage(empresa, ((Number) l.get("tokens")).longValue(), String.valueOf(l.get("model_name"))));
            }
        }
        usos.saveAll(novos);
        log.info("Demonstração: {} registros de consumo de IA criados.", novos.size());
    }

    /** Nomes das ferramentas aprovadas por código de empresa (approved_ai_tools.csv + ai_tools.csv). */
    Map<String, List<String>> ferramentasAprovadas() {
        Map<String, String> nomes = new HashMap<>();
        base.tabela("ai_tools.csv").linhas().forEach(l -> nomes.put(l.get("ai_tool_id"), l.get("tool_name")));
        Map<String, List<String>> r = new HashMap<>();
        for (Map<String, String> l : base.tabela("approved_ai_tools.csv").linhas()) {
            if ("Aprovado".equalsIgnoreCase(l.get("approval_status"))) {
                r.computeIfAbsent(l.get("company_id"), k -> new ArrayList<>())
                        .add(nomes.getOrDefault(l.get("ai_tool_id"), l.get("ai_tool_id")));
            }
        }
        return r;
    }

    /** Colaboradores que concluíram treinamento de Governança ou de Segurança e Compliance. */
    Set<String> treinadosEmGovernanca() {
        Set<String> r = new HashSet<>();
        for (Map<String, String> l : base.tabela(BaseDadosCsv.TREINAMENTOS).linhas()) {
            String categoria = semAcento(l.get("course_category"));
            if (CATEGORIAS_GOVERNANCA.contains(categoria) && "concluido".equals(semAcento(l.get("completion_status")))) {
                r.add(l.get("employee_id"));
            }
        }
        return r;
    }

    static List<PoliticaRequest> textos(Empresa empresa, List<String> ferramentas) {
        String lista = ferramentas.isEmpty() ? "nenhuma ferramenta aprovada até o momento"
                : String.join(", ", ferramentas);
        return List.of(
                new PoliticaRequest(empresa.getId(), "Uso responsável de IA generativa",
                        "Na " + empresa.getNome() + ", ferramentas de IA apoiam o trabalho, mas a responsabilidade "
                        + "pelo resultado é sempre do colaborador. Use apenas ferramentas aprovadas, confira fatos, "
                        + "números e código gerados e informe quando um entregável teve apoio de IA.", true),
                new PoliticaRequest(empresa.getId(), "Proteção de dados pessoais (LGPD) no uso de IA",
                        "É proibido inserir em ferramentas de IA dados pessoais de clientes ou colaboradores, dados "
                        + "financeiros sigilosos, senhas ou código proprietário sem anonimização. Incidentes devem ser "
                        + "comunicados à área de Segurança da Informação em até 24 horas.", true),
                new PoliticaRequest(empresa.getId(), "Ferramentas de IA aprovadas",
                        "Ferramentas liberadas para uso corporativo: " + lista + ". Pedidos de novas ferramentas "
                        + "passam por avaliação de segurança e custos antes da liberação.", false));
    }

    private static String semAcento(String texto) {
        return java.text.Normalizer.normalize(texto == null ? "" : texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
