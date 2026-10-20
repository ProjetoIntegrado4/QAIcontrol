package br.edu.exemplo.ia.repository.produtividade;

/**
 * Trechos de consulta (CTEs) reaproveitados por mais de uma seção. Leem as tabelas pelos nomes
 * lógicos de produtividade.yml e expõem colunas com nomes lógicos, então o restante das consultas
 * não depende do banco físico. Todos filtram por :emp (id da empresa ou 'ALL').
 */
final class Fragmentos {

    private Fragmentos() {
    }

    /** Uso de IA da empresa, com as colunas já convertidas. */
    static final String USO = """
            uso as (
              select l.${uso.funcionario} as funcionario, l.${uso.empresa} as empresa,
                     l.${uso.departamento} as departamento, l.${uso.ferramenta} as ferramenta,
                     l.${uso.data}::timestamp as ts, to_char(l.${uso.data}::timestamp, 'YYYY-MM') as mes,
                     l.${uso.requisicoes}::numeric as rc, l.${uso.status} in (:uso_sucesso) as ok,
                     coalesce(l.${uso.tokens_entrada}::numeric, 0) + coalesce(l.${uso.tokens_saida}::numeric, 0) as tokens,
                     l.${uso.latencia}::numeric as latencia
              from ${uso} l
              where (:emp = 'ALL' or l.${uso.empresa} = :emp))
            """;

    /** Meses cobertos pela base de uso inteira (eixo dos gráficos mensais). */
    static final String MESES = """
            meses as (select distinct to_char(${uso.data}::timestamp, 'YYYY-MM') as mes from ${uso})
            """;

    /** Data final da base e pares funcionário x ferramenta com uso bem-sucedido na janela recente. */
    static final String RECENTE = """
            fim as (select max(${uso.data}::timestamp) as fim from ${uso}),
            recente as (
              select distinct u.${uso.funcionario} as funcionario, u.${uso.ferramenta} as ferramenta
              from ${uso} u, fim
              where u.${uso.status} in (:uso_sucesso)
                and u.${uso.data}::timestamp >= fim.fim - make_interval(days => :janela_dias))
            """;

    /** Colaboradores ativos do cadastro. */
    static final String ATIVOS = """
            ativos as (
              select e.${funcionarios.id} as funcionario, e.${funcionarios.departamento} as departamento
              from ${funcionarios} e
              where lower(e.${funcionarios.ativo}::text) in (:funcionario_ativo)
                and (:emp = 'ALL' or e.${funcionarios.empresa} = :emp))
            """;

    /** Conformidade com a política: ferramenta aprovada na empresa e área entre as autorizadas. Requer USO. */
    static final String POLITICA = """
            pol as (
              select k.empresa, k.ferramenta, k.departamento,
                     exists (select 1 from ${ferramentas_aprovadas} a
                             where a.${ferramentas_aprovadas.empresa} = k.empresa
                               and a.${ferramentas_aprovadas.ferramenta} = k.ferramenta
                               and a.${ferramentas_aprovadas.status} in (:aprovacao_aprovada)
                               and exists (select 1 from regexp_split_to_table(a.${ferramentas_aprovadas.areas}, '[,;]') x
                                           where trim(x) in (:aprovacao_todas_areas)
                                              or trim(x) = d.${departamentos.nome})) as conforme
              from (select distinct empresa, ferramenta, departamento from uso) k
              left join ${departamentos} d on d.${departamentos.id} = k.departamento),
            usop as (
              select k.ferramenta, k.req_ok, coalesce(p.conforme, false) as conforme
              from (select empresa, ferramenta, departamento, sum(rc) filter (where ok) as req_ok
                    from uso group by 1, 2, 3) k
              left join pol p on p.empresa = k.empresa and p.ferramenta = k.ferramenta
                             and coalesce(p.departamento, '') = coalesce(k.departamento, ''))
            """;

    /** Licenças por ferramenta: ativas e ociosas (ativas sem uso do titular na janela recente). Requer RECENTE. */
    static final String LICENCAS = """
            lic as (
              select l.${licencas.ferramenta} as ferramenta,
                     count(*) filter (where l.${licencas.status} in (:licenca_ativa)) as ativas,
                     count(*) filter (where l.${licencas.status} in (:licenca_ativa) and r.funcionario is null) as ociosas
              from ${licencas} l
              left join recente r on r.funcionario = l.${licencas.funcionario} and r.ferramenta = l.${licencas.ferramenta}
              where (:emp = 'ALL' or l.${licencas.empresa} = :emp)
              group by 1)
            """;

    /** Projetos da empresa e seus pull requests (a empresa vem do projeto). */
    static final String PRS = """
            proj as (
              select ${projetos.id} as projeto from ${projetos} where (:emp = 'ALL' or ${projetos.empresa} = :emp)
              union
              select distinct ${pull_requests.projeto} from ${pull_requests} where :emp = 'ALL'),
            pr as (
              select p.${pull_requests.funcionario} as funcionario,
                     to_char(p.${pull_requests.criado_em}::timestamp, 'YYYY-MM') as mes,
                     p.${pull_requests.status} in (:pr_mergeado) as mergeado,
                     case when p.${pull_requests.status} in (:pr_mergeado)
                               and p.${pull_requests.mergeado_em}::timestamp >= p.${pull_requests.criado_em}::timestamp
                          then extract(epoch from p.${pull_requests.mergeado_em}::timestamp
                                                  - p.${pull_requests.criado_em}::timestamp) / 3600 end as lead_h
              from ${pull_requests} p join proj on proj.projeto = p.${pull_requests.projeto})
            """;

    /** Builds dos projetos da empresa. Requer PRS (usa proj). */
    static final String BUILDS = """
            bl as (
              select to_char(b.${builds.data}::timestamp, 'YYYY-MM') as mes, b.${builds.status} in (:build_sucesso) as ok
              from ${builds} b join proj on proj.projeto = b.${builds.projeto})
            """;

    /** Deploys dos projetos da empresa; falha = qualquer status fora de deploy-sucesso. Requer PRS. */
    static final String DEPLOYS = """
            dp as (
              select to_char(d.${deploys.data}::timestamp, 'YYYY-MM') as mes,
                     coalesce(d.${deploys.status} not in (:deploy_sucesso), true) as falha
              from ${deploys} d join proj on proj.projeto = d.${deploys.projeto})
            """;

    /**
     * Desenvolvedores divididos em três faixas iguais pelo volume de requisições de IA
     * (empates desfeitos pelo id). Requer USO e PRS.
     */
    static final String COORTES = """
            devs as (
              select funcionario, count(*) as prs, count(*) filter (where mergeado) as merged,
                     percentile_cont(0.5) within group (order by lead_h) as lead
              from pr group by funcionario),
            ia as (select funcionario, sum(rc) as ai from uso where ok group by funcionario),
            ord as (
              select d.*, coalesce(ia.ai, 0)::float8 as ai,
                     row_number() over (order by coalesce(ia.ai, 0), d.funcionario) as r,
                     count(*) over () as n
              from devs d left join ia on ia.funcionario = d.funcionario),
            faixas as (
              select *, case when r <= 1 + (n - 1) / 3.0 then 1
                             when r <= 1 + 2 * (n - 1) / 3.0 then 2 else 3 end as faixa
              from ord)
            """;

    static String com(String... ctes) {
        return "with " + String.join(", ", ctes);
    }
}
