package br.edu.exemplo.ia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Carrega a base original de uso de IA (os 17 CSVs) no schema public do PostgreSQL.
 * Roda na subida da aplicação e só age quando a tabela não existe ou está vazia:
 * tabelas que já têm dados (importadas antes ou alimentadas por outro processo) não são tocadas.
 * Pasta dos CSVs: variável PRODUTIVIDADE_CSV_DIR (padrão dados/csv).
 */
@Component
public class ProdutividadeCsvImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdutividadeCsvImporter.class);
    private static final int LINHAS_POR_INSERT = 500;

    /** Tabelas na ordem de carga, com o tipo de cada coluna (mesma ordem do cabeçalho do CSV). */
    static final Map<String, Map<String, String>> TABELAS = new LinkedHashMap<>();

    static {
        tabela("companies", "company_id text primary key", "company_name text", "industry text",
                "company_size text", "employee_count integer");
        tabela("departments", "department_id text primary key", "company_id text", "department_name text");
        tabela("employees", "employee_id text primary key", "company_id text", "department_id text", "job_role text",
                "seniority text", "hire_date date", "cost_center text", "active boolean");
        tabela("ai_tools", "ai_tool_id text primary key", "tool_name text", "provider text", "tool_category text",
                "licensing_type text");
        tabela("ai_models", "model_id text primary key", "ai_tool_id text", "model_name text", "model_family text",
                "input_price_usd_per_1k_tokens numeric", "output_price_usd_per_1k_tokens numeric");
        tabela("approved_ai_tools", "company_id text", "ai_tool_id text", "approval_status text", "approval_date date",
                "allowed_departments text");
        tabela("ai_licenses", "company_id text", "employee_id text", "ai_tool_id text", "license_type text",
                "assigned_date date", "expiration_date date", "license_status text");
        tabela("ai_billing", "billing_id text primary key", "company_id text", "billing_month text", "provider text",
                "ai_tool_id text", "model_id text", "cost_center text", "usage_quantity bigint", "total_cost numeric",
                "currency text");
        tabela("ai_budgets", "company_id text", "cost_center text", "year integer", "month integer",
                "budget_amount numeric");
        tabela("ai_usage_logs", "usage_id text primary key", "timestamp timestamp", "employee_id text",
                "company_id text", "department_id text", "ai_tool_id text", "model_id text", "application text",
                "request_count integer", "input_tokens bigint", "output_tokens bigint", "latency_ms numeric",
                "request_status text", "project_id text", "cost_center text");
        tabela("training_records", "training_id text primary key", "employee_id text", "company_id text",
                "course_name text", "course_category text", "enrollment_date date", "completion_date date",
                "completion_status text", "score numeric", "duration_hours numeric");
        tabela("security_events", "event_id text primary key", "company_id text", "employee_id text",
                "timestamp timestamp", "ai_tool_id text", "event_type text", "severity text", "policy_id text",
                "action_taken text");
        tabela("projects", "project_id text primary key", "company_id text", "project_name text", "project_type text",
                "start_date date", "status text");
        tabela("pull_requests", "pull_request_id text primary key", "project_id text", "employee_id text",
                "created_at timestamp", "merged_at timestamp", "status text", "files_changed integer",
                "additions integer", "deletions integer", "review_count numeric");
        tabela("builds", "build_id text primary key", "project_id text", "timestamp timestamp", "status text",
                "duration integer", "tests_executed integer", "tests_failed integer");
        tabela("deployments", "deployment_id text primary key", "project_id text", "timestamp timestamp",
                "environment text", "status text");
        tabela("issues", "issue_id text primary key", "project_id text", "employee_id text", "issue_type text",
                "priority text", "created_at timestamp", "closed_at timestamp", "status text");
    }

    private static final List<String> INDICES = List.of(
            "create index if not exists ix_ai_usage_logs_company on ai_usage_logs (company_id)",
            "create index if not exists ix_ai_usage_logs_emp_tool on ai_usage_logs (employee_id, ai_tool_id)",
            "create index if not exists ix_employees_company on employees (company_id)",
            "create index if not exists ix_ai_billing_company on ai_billing (company_id)",
            "create index if not exists ix_pull_requests_project on pull_requests (project_id)");

    private static void tabela(String nome, String... colunas) {
        Map<String, String> c = new LinkedHashMap<>();
        for (String col : colunas) {
            int i = col.indexOf(' ');
            c.put(col.substring(0, i), col.substring(i + 1));
        }
        TABELAS.put(nome, c);
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Path pasta;

    public ProdutividadeCsvImporter(JdbcTemplate jdbc, TransactionTemplate tx,
                                    @Value("${PRODUTIVIDADE_CSV_DIR:dados/csv}") String pasta) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.pasta = Path.of(pasta);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Files.isDirectory(pasta)) {
            log.info("Produtividade: pasta de CSVs {} não encontrada; usando apenas o que já está no banco.", pasta.toAbsolutePath());
            return;
        }
        boolean criouAlguma = false;
        for (var t : TABELAS.entrySet()) {
            Path csv = pasta.resolve(t.getKey() + ".csv");
            try {
                criouAlguma |= carregar(t.getKey(), t.getValue(), csv);
            } catch (Exception e) {
                log.error("Produtividade: falha ao importar {}: {}", csv, e.getMessage());
            }
        }
        if (criouAlguma) INDICES.forEach(jdbc::execute);
    }

    /** Retorna true quando a tabela foi criada agora. */
    private boolean carregar(String nome, Map<String, String> colunas, Path csv) throws IOException {
        boolean existe = Boolean.TRUE.equals(jdbc.queryForObject("select to_regclass(?) is not null", Boolean.class, "public." + nome));
        if (existe) {
            Boolean temDados = jdbc.queryForObject("select exists (select 1 from public." + nome + ")", Boolean.class);
            if (Boolean.TRUE.equals(temDados)) return false;
        }
        if (!Files.isRegularFile(csv)) {
            if (!existe) log.warn("Produtividade: tabela {} não existe e {} não foi encontrado.", nome, csv);
            return false;
        }
        long inicio = System.currentTimeMillis();
        List<String> cols = new ArrayList<>(colunas.keySet());
        long[] total = {0};
        tx.executeWithoutResult(s -> {
            if (!existe) {
                jdbc.execute("create table public." + nome + " (" + colunas.entrySet().stream()
                        .map(c -> q(c.getKey()) + " " + c.getValue()).collect(Collectors.joining(", ")) + ")");
            }
            try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
                List<String> cab = parse(in.readLine().replace("﻿", ""));
                if (!cab.equals(cols)) throw new IllegalStateException("cabeçalho diferente do esperado: " + cab);
                List<List<String>> lote = new ArrayList<>();
                String linha;
                while ((linha = in.readLine()) != null) {
                    if (linha.isBlank()) continue;
                    lote.add(parse(linha));
                    if (lote.size() == LINHAS_POR_INSERT) total[0] += inserir(nome, colunas, lote);
                }
                total[0] += inserir(nome, colunas, lote);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        jdbc.execute("analyze public." + nome);
        log.info("Produtividade: {} linhas importadas em {} ({} ms).", total[0], nome, System.currentTimeMillis() - inicio);
        return !existe;
    }

    private int inserir(String nome, Map<String, String> colunas, List<List<String>> lote) {
        if (lote.isEmpty()) return 0;
        String valores = colunas.values().stream()
                .map(tipo -> "cast(nullif(?, '') as " + tipo.replace(" primary key", "") + ")")
                .collect(Collectors.joining(", ", "(", ")"));
        String sql = "insert into public." + nome + " (" + colunas.keySet().stream().map(ProdutividadeCsvImporter::q)
                .collect(Collectors.joining(", ")) + ") values " + String.join(", ", Collections.nCopies(lote.size(), valores));
        int n = colunas.size();
        Object[] params = new Object[lote.size() * n];
        for (int i = 0; i < lote.size(); i++) {
            List<String> r = lote.get(i);
            for (int j = 0; j < n; j++) params[i * n + j] = j < r.size() ? r.get(j) : null;
        }
        int linhas = lote.size();
        jdbc.update(sql, params);
        lote.clear();
        return linhas;
    }

    private static String q(String coluna) {
        return "\"" + coluna + "\"";
    }

    /** CSV simples (RFC 4180) em uma linha: vírgula como separador e aspas duplas para campos com vírgula. */
    static List<String> parse(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (aspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') { atual.append('"'); i++; }
                else if (c == '"') aspas = false;
                else atual.append(c);
            } else if (c == '"') aspas = true;
            else if (c == ',') { campos.add(atual.toString()); atual.setLength(0); }
            else atual.append(c);
        }
        campos.add(atual.toString());
        return campos;
    }
}
