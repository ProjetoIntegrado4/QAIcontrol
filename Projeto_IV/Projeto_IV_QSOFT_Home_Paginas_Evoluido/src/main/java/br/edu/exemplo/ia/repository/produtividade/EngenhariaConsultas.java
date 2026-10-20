package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.BUILDS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.COORTES;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.DEPLOYS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.MESES;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.PRS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.USO;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.com;

/** Entrega de software: pull requests, builds e deploys dos projetos da empresa. */
@Repository
public class EngenhariaConsultas {

    private final ConsultaPainel db;

    public EngenhariaConsultas(ConsultaPainel db) {
        this.db = db;
    }

    /** Empresas que têm projetos cadastrados. */
    public List<String> empresasComProjetos() {
        return db.lista("select distinct ${projetos.empresa} as id from ${projetos} order by 1", "ALL")
                .stream().map(l -> String.valueOf(l.get("id"))).toList();
    }

    public Map<String, Object> totaisPullRequests(String empresa) {
        return db.linha(com(PRS) + """
                select count(*) as prs, count(*) filter (where mergeado) as merged,
                       percentile_cont(0.5) within group (order by lead_h) as lead,
                       count(distinct funcionario) as devs
                from pr
                """, empresa);
    }

    public Map<String, Object> totaisBuilds(String empresa) {
        return db.linha(com(PRS, BUILDS) + "select 100.0 * avg(ok::int) as build_ok from bl", empresa);
    }

    public Map<String, Object> totaisDeploys(String empresa) {
        return db.linha(com(PRS, DEPLOYS) + "select 100.0 * avg(falha::int) as deploy_falha from dp", empresa);
    }

    /** Pull requests por mês, com todos os meses da base de uso. */
    public List<Map<String, Object>> pullRequestsPorMes(String empresa) {
        return db.lista(com(PRS, MESES) + """
                , pm as (select mes, count(*) as prs, count(*) filter (where mergeado) as merged,
                                percentile_cont(0.5) within group (order by lead_h) as lead
                         from pr group by mes)
                select m.mes, coalesce(pm.prs, 0) as prs, coalesce(pm.merged, 0) as merged, pm.lead
                from meses m left join pm on pm.mes = m.mes order by m.mes
                """, empresa);
    }

    public List<Map<String, Object>> buildsPorMes(String empresa) {
        return db.lista(com(PRS, BUILDS) + "select mes, 100.0 * avg(ok::int) as build_ok from bl group by mes", empresa);
    }

    public List<Map<String, Object>> deploysPorMes(String empresa) {
        return db.lista(com(PRS, DEPLOYS) + "select mes, 100.0 * avg(falha::int) as deploy_falha from dp group by mes",
                empresa);
    }

    public List<Map<String, Object>> coortes(String empresa) {
        return db.lista(com(USO, PRS, COORTES) + """
                select faixa, count(*) as devs,
                       percentile_cont(0.5) within group (order by ai) as ai_mediana,
                       avg(prs) as prs, avg(merged) as merged,
                       percentile_cont(0.5) within group (order by lead) as lead
                from faixas group by faixa order by faixa
                """, empresa);
    }

    public Double correlacao(String empresa) {
        return db.valor(com(USO, PRS, COORTES) + "select corr(ai, merged) from faixas", empresa, Double.class);
    }
}
