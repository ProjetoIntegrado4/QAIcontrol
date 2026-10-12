package br.edu.exemplo.ia.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Consultas do painel de produtividade sobre a base original de uso de IA (schema public:
 * ai_usage_logs, employees, departments, ai_billing, ai_licenses, pull_requests...).
 * Todas recebem :emp = id da empresa (ex.: C01) ou 'ALL' para o consolidado.
 * Os valores são lidos a cada chamada, então registros novos aparecem no painel sem reprocessamento.
 */
@Repository
public class ProdutividadeRepository {

    /** Uso filtrado pela empresa, com as colunas já convertidas. */
    private static final String USO = """
            uso as (
              select l.employee_id, l.company_id, l.department_id, l.ai_tool_id,
                     l."timestamp"::timestamp as ts, to_char(l."timestamp"::timestamp, 'YYYY-MM') as mes,
                     l.request_count::numeric as rc, l.request_status = 'success' as ok,
                     coalesce(l.input_tokens::numeric, 0) + coalesce(l.output_tokens::numeric, 0) as tokens,
                     l.latency_ms::numeric as latency_ms
              from ai_usage_logs l
              where (:emp = 'ALL' or l.company_id = :emp))
            """;

    /** Data final da base e pares funcionário x ferramenta com uso bem-sucedido nos 90 dias anteriores. */
    private static final String RECENTE = """
            fim as (select max("timestamp"::timestamp) as fim from ai_usage_logs),
            recente as (
              select distinct employee_id, ai_tool_id from ai_usage_logs, fim
              where request_status = 'success' and "timestamp"::timestamp >= fim.fim - interval '90 days')
            """;

    /** Conformidade com a política: ferramenta aprovada na empresa e área entre as autorizadas. */
    private static final String POLITICA = """
            pol as (
              select k.company_id, k.ai_tool_id, k.department_id,
                     exists (select 1 from approved_ai_tools a
                             where a.company_id = k.company_id and a.ai_tool_id = k.ai_tool_id
                               and a.approval_status = 'Aprovado'
                               and (a.allowed_departments like 'Todos%'
                                    or exists (select 1 from regexp_split_to_table(a.allowed_departments, '[,;]') x
                                               where trim(x) = d.department_name))) as conforme
              from (select distinct company_id, ai_tool_id, department_id from uso) k
              left join departments d on d.department_id = k.department_id),
            usop as (
              select k.ai_tool_id, k.req_ok, coalesce(p.conforme, false) as conforme
              from (select company_id, ai_tool_id, department_id, sum(rc) filter (where ok) as req_ok
                    from uso group by 1, 2, 3) k
              left join pol p on p.company_id = k.company_id and p.ai_tool_id = k.ai_tool_id
                             and coalesce(p.department_id, '') = coalesce(k.department_id, ''))
            """;

    /** Licenças por ferramenta: ativas e ociosas (ativas sem uso do titular nos últimos 90 dias). */
    private static final String LICENCAS = """
            lic as (
              select l.ai_tool_id,
                     count(*) filter (where l.license_status = 'Ativa') as ativas,
                     count(*) filter (where l.license_status = 'Ativa' and r.employee_id is null) as ociosas
              from ai_licenses l
              left join recente r on r.employee_id = l.employee_id and r.ai_tool_id = l.ai_tool_id
              where (:emp = 'ALL' or l.company_id = :emp)
              group by l.ai_tool_id)
            """;

    private static final String ATIVO = "lower(e.active::text) in ('true', 't', '1')";

    /** Pull requests, builds e deploys da empresa (a empresa vem do projeto). */
    private static final String ENG = """
            proj as (select project_id from projects where (:emp = 'ALL' or company_id = :emp)
                     union select distinct project_id from pull_requests where :emp = 'ALL'),
            pr as (
              select p.pull_request_id, p.employee_id, p.status, to_char(p.created_at::timestamp, 'YYYY-MM') as mes,
                     case when p.status = 'Merged' and p.merged_at::timestamp >= p.created_at::timestamp
                          then extract(epoch from p.merged_at::timestamp - p.created_at::timestamp) / 3600 end as lead_h
              from pull_requests p join proj using (project_id)),
            bl as (select to_char(b."timestamp"::timestamp, 'YYYY-MM') as mes, b.status = 'Sucesso' as ok
                   from builds b join proj using (project_id)),
            dp as (select to_char(d."timestamp"::timestamp, 'YYYY-MM') as mes, d.status is distinct from 'Sucesso' as falha
                   from deployments d join proj using (project_id))
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public ProdutividadeRepository(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /**
     * Executa as consultas do painel numa transação de leitura com o JIT do PostgreSQL desligado:
     * nessas agregações a compilação JIT custa mais do que a própria consulta.
     */
    public <T> T consultar(Supplier<T> consultas) {
        return tx.execute(s -> {
            jdbc.getJdbcTemplate().execute("set local jit = off");
            return consultas.get();
        });
    }

    private static MapSqlParameterSource emp(String empresa) {
        return new MapSqlParameterSource("emp", empresa);
    }

    private List<Map<String, Object>> lista(String sql, String empresa) {
        return jdbc.queryForList(sql, emp(empresa));
    }

    public List<Map<String, Object>> empresas() {
        return jdbc.queryForList("""
                select company_id as id, company_name as nome, industry as setor, company_size as porte
                from companies order by company_id""", Map.of());
    }

    public Map<String, Object> periodo() {
        return jdbc.queryForMap("""
                select to_char(min("timestamp"::timestamp), 'YYYY-MM') as inicio,
                       to_char(max("timestamp"::timestamp), 'YYYY-MM') as fim,
                       count(*) as linhas_uso, count(*) filter (where cost_center is null) as uso_sem_cc
                from ai_usage_logs""", Map.of());
    }

    public Map<String, Object> qualidade() {
        return jdbc.queryForMap("""
                select (select count(*) from pull_requests
                        where merged_at::timestamp < created_at::timestamp) as lead_negativo,
                       (select count(*) from pull_requests
                        where review_count::numeric <> trunc(review_count::numeric)) as review_imputado,
                       (select count(*) from employees where cost_center is null) as sem_cc""", Map.of());
    }

    public Map<String, Object> kpis(String empresa) {
        return jdbc.queryForMap("with " + USO + ", " + RECENTE + ", " + POLITICA + ", " + LICENCAS + """
                , ativos as (select e.employee_id from employees e
                             where %s and (:emp = 'ALL' or e.company_id = :emp)),
                trein as (select * from training_records where (:emp = 'ALL' or company_id = :emp)),
                ev as (select * from security_events where (:emp = 'ALL' or company_id = :emp))
                select
                  (select count(*) from ativos) as funcionarios,
                  (select count(distinct employee_id) from uso where ok) as usuarios,
                  (select count(distinct u.employee_id) from uso u join ativos a using (employee_id) where u.ok) as usuarios_ativos,
                  (select coalesce(sum(rc) filter (where ok), 0) from uso) as requisicoes,
                  (select coalesce(sum(tokens) filter (where ok), 0) from uso) as tokens,
                  (select sum(rc) from uso) as requisicoes_total,
                  (select percentile_cont(0.5) within group (order by latency_ms) filter (where ok) from uso) as latencia,
                  (select coalesce(sum(req_ok) filter (where not conforme), 0) from usop) as req_fora,
                  (select coalesce(sum(total_cost::numeric), 0) from ai_billing where (:emp = 'ALL' or company_id = :emp)) as custo,
                  (select coalesce(sum(budget_amount::numeric), 0) from ai_budgets where (:emp = 'ALL' or company_id = :emp)) as orcamento,
                  (select coalesce(sum(ativas), 0) from lic) as lic_ativas,
                  (select coalesce(sum(ociosas), 0) from lic) as lic_ociosas,
                  (select count(*) from trein) as trein_total,
                  (select count(*) filter (where completion_status = 'Concluido') from trein) as trein_concluidos,
                  (select count(distinct employee_id) filter (where completion_status = 'Concluido') from trein) as trein_pessoas,
                  (select count(*) from ev) as eventos,
                  (select count(*) filter (where severity in ('Alta', 'Critica')) from ev) as eventos_graves
                """.formatted(ATIVO), emp(empresa));
    }

    public List<Map<String, Object>> mensal(String empresa) {
        return lista("with " + USO + """
                , meses as (select distinct to_char("timestamp"::timestamp, 'YYYY-MM') as mes from ai_usage_logs),
                um as (select mes, sum(rc) filter (where ok) as req, count(distinct employee_id) filter (where ok) as ativos,
                              sum(rc) as total
                       from uso group by mes),
                bm as (select left(billing_month::text, 7) as mes, sum(total_cost::numeric) as custo
                       from ai_billing where (:emp = 'ALL' or company_id = :emp) group by 1),
                om as (select year::int || '-' || lpad(month::int::text, 2, '0') as mes, sum(budget_amount::numeric) as orcamento
                       from ai_budgets where (:emp = 'ALL' or company_id = :emp) group by 1)
                select m.mes, coalesce(um.req, 0) as req, coalesce(um.ativos, 0) as ativos,
                       case when um.total > 0 then 100.0 * um.req / um.total end as sucesso,
                       coalesce(bm.custo, 0) as custo, coalesce(om.orcamento, 0) as orcamento
                from meses m left join um using (mes) left join bm using (mes) left join om using (mes)
                order by m.mes
                """, empresa);
    }

    /** Áreas agrupadas pelo nome do departamento (no consolidado, a mesma área de empresas diferentes soma). */
    public List<Map<String, Object>> areas(String empresa) {
        return lista("with " + USO + """
                , ea as (select e.employee_id, d.department_name as area from employees e
                         join departments d on d.department_id = e.department_id
                         where %s and (:emp = 'ALL' or e.company_id = :emp)),
                ua as (select d.department_name as area, u.employee_id, u.rc from uso u
                       join departments d on d.department_id = u.department_id where u.ok),
                req as (select area, sum(rc) as req, count(distinct employee_id) as usuarios_uso from ua group by area),
                usu as (select ua.area, count(distinct ua.employee_id) as usuarios from ua
                        join ea on ea.employee_id = ua.employee_id and ea.area = ua.area group by ua.area),
                cst as (select d.department_name as area, sum(b.total_cost::numeric) as custo from ai_billing b
                        join departments d on d.department_id = replace(b.cost_center, 'CC-', '')
                        where (:emp = 'ALL' or b.company_id = :emp) group by 1),
                trn as (select d.department_name as area,
                               100.0 * avg((t.completion_status = 'Concluido')::int) as trein
                        from training_records t join employees e on e.employee_id = t.employee_id
                        join departments d on d.department_id = e.department_id
                        where (:emp = 'ALL' or t.company_id = :emp) group by 1)
                select ea.area, count(*) as funcionarios, coalesce(usu.usuarios, 0) as usuarios,
                       coalesce(req.req, 0) as req,
                       case when req.usuarios_uso > 0 then req.req / req.usuarios_uso else 0 end as req_usuario,
                       coalesce(cst.custo, 0) as custo, trn.trein
                from ea left join usu using (area) left join req using (area) left join cst using (area) left join trn using (area)
                group by ea.area, usu.usuarios, req.req, req.usuarios_uso, cst.custo, trn.trein
                order by req desc, ea.area
                """.formatted(ATIVO), empresa);
    }

    public List<Map<String, Object>> ferramentas(String empresa) {
        return lista("with " + USO + ", " + RECENTE + ", " + POLITICA + ", " + LICENCAS + """
                , f as (select ai_tool_id, sum(rc) as req, count(distinct employee_id) as usuarios
                        from uso where ok group by ai_tool_id),
                fp as (select ai_tool_id, sum(req_ok) filter (where not conforme) as req_fora from usop group by ai_tool_id),
                c as (select ai_tool_id, sum(total_cost::numeric) as custo from ai_billing
                      where (:emp = 'ALL' or company_id = :emp) group by ai_tool_id)
                select f.ai_tool_id as id, coalesce(t.tool_name, f.ai_tool_id) as nome, t.tool_category as categoria,
                       f.req, f.usuarios, coalesce(c.custo, 0) as custo,
                       coalesce(lic.ativas, 0) as lic_ativas, coalesce(lic.ociosas, 0) as lic_ociosas,
                       100.0 * coalesce(fp.req_fora, 0) / nullif(f.req, 0) as fora
                from f left join fp using (ai_tool_id) left join ai_tools t using (ai_tool_id) left join c using (ai_tool_id)
                left join lic using (ai_tool_id)
                order by f.req desc
                """, empresa);
    }

    /** Doze colaboradores com mais requisições bem-sucedidas nos últimos 90 dias da base. */
    public List<Map<String, Object>> top(String empresa) {
        return lista("with " + USO + ", " + RECENTE + """
                , r as (select employee_id, sum(rc) as req, count(distinct ai_tool_id) as ferramentas,
                               count(distinct ts::date) as dias
                        from uso, fim where ok and ts >= fim.fim - interval '90 days' group by employee_id)
                select r.employee_id as id, e.job_role as cargo, e.seniority as senioridade, d.department_name as area,
                       r.req, r.ferramentas, r.dias
                from r left join employees e using (employee_id) left join departments d on d.department_id = e.department_id
                order by r.req desc, r.employee_id limit 12
                """, empresa);
    }

    public List<Map<String, Object>> eventosPorTipo(String empresa) {
        return lista("""
                select event_type as chave, count(*) as total from security_events
                where (:emp = 'ALL' or company_id = :emp) group by 1 order by 2 desc, 1""", empresa);
    }

    public List<Map<String, Object>> eventosPorSeveridade(String empresa) {
        return lista("""
                select severity as chave, count(*) as total from security_events
                where (:emp = 'ALL' or company_id = :emp) group by 1 order by 2 desc, 1""", empresa);
    }

    public List<Map<String, Object>> treinamentosPorSituacao(String empresa) {
        return lista("""
                select completion_status as chave, count(*) as total from training_records
                where (:emp = 'ALL' or company_id = :emp) group by 1 order by 2 desc, 1""", empresa);
    }

    public List<Map<String, Object>> engMensal(String empresa) {
        return lista("with " + ENG + """
                , meses as (select distinct to_char("timestamp"::timestamp, 'YYYY-MM') as mes from ai_usage_logs),
                pm as (select mes, count(*) as prs, count(*) filter (where status = 'Merged') as merged,
                              percentile_cont(0.5) within group (order by lead_h) as lead
                       from pr group by mes),
                bm as (select mes, 100.0 * avg(ok::int) as build_ok from bl group by mes),
                dm as (select mes, 100.0 * avg(falha::int) as deploy_falha from dp group by mes)
                select m.mes, coalesce(pm.prs, 0) as prs, coalesce(pm.merged, 0) as merged, pm.lead,
                       bm.build_ok, dm.deploy_falha
                from meses m left join pm using (mes) left join bm using (mes) left join dm using (mes)
                order by m.mes
                """, empresa);
    }

    public Map<String, Object> engTotais(String empresa) {
        return jdbc.queryForMap("with " + ENG + """
                select (select count(*) from pr) as prs,
                       (select count(*) filter (where status = 'Merged') from pr) as merged,
                       (select percentile_cont(0.5) within group (order by lead_h) from pr) as lead,
                       (select 100.0 * avg(ok::int) from bl) as build_ok,
                       (select 100.0 * avg(falha::int) from dp) as deploy_falha,
                       (select count(distinct employee_id) from pr) as devs
                """, emp(empresa));
    }

    /**
     * Desenvolvedores divididos em três faixas iguais pelo volume de requisições de IA
     * (empates desfeitos pelo id), com a produção de PRs de cada faixa.
     */
    public List<Map<String, Object>> engCoortes(String empresa) {
        return lista("with " + USO + ", " + ENG + COORTE_DEVS + """
                select faixa, count(*) as devs,
                       percentile_cont(0.5) within group (order by ai) as ai_mediana,
                       avg(prs) as prs, avg(merged) as merged,
                       percentile_cont(0.5) within group (order by lead) as lead
                from faixas group by faixa order by faixa
                """, empresa);
    }

    public Double engCorrelacao(String empresa) {
        return jdbc.queryForObject("with " + USO + ", " + ENG + COORTE_DEVS + """
                select corr(ai, merged) from faixas""", emp(empresa), Double.class);
    }

    private static final String COORTE_DEVS = """
            , devs as (select employee_id, count(*) as prs, count(*) filter (where status = 'Merged') as merged,
                              percentile_cont(0.5) within group (order by lead_h) as lead
                       from pr group by employee_id),
            ia as (select employee_id, sum(rc) as ai from uso where ok group by employee_id),
            ord as (select d.*, coalesce(ia.ai, 0)::float8 as ai,
                           row_number() over (order by coalesce(ia.ai, 0), d.employee_id) as r,
                           count(*) over () as n
                    from devs d left join ia using (employee_id)),
            faixas as (select *, case when r <= 1 + (n - 1) / 3.0 then 1
                                      when r <= 1 + 2 * (n - 1) / 3.0 then 2 else 3 end as faixa
                       from ord)
            """;
}
