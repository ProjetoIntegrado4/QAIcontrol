package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.Map;

import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.ATIVOS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.LICENCAS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.POLITICA;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.RECENTE;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.USO;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.com;

/** Totais da faixa de indicadores. Cada bloco depende de tabelas diferentes e pode faltar sozinho. */
@Repository
public class IndicadoresConsultas {

    private final ConsultaPainel db;

    public IndicadoresConsultas(ConsultaPainel db) {
        this.db = db;
    }

    /** Colaboradores, usuários, requisições, tokens e latência. */
    public Map<String, Object> uso(String empresa) {
        return db.linha(com(USO, ATIVOS) + """
                select
                  (select count(*) from ativos) as funcionarios,
                  (select count(distinct funcionario) from uso where ok) as usuarios,
                  (select count(distinct u.funcionario) from uso u join ativos a on a.funcionario = u.funcionario
                   where u.ok) as usuarios_ativos,
                  (select coalesce(sum(rc) filter (where ok), 0) from uso) as requisicoes,
                  (select coalesce(sum(tokens) filter (where ok), 0) from uso) as tokens,
                  (select sum(rc) from uso) as requisicoes_total,
                  (select percentile_cont(0.5) within group (order by latencia) filter (where ok) from uso) as latencia
                """, empresa);
    }

    /** Requisições bem-sucedidas com ferramenta fora da política. */
    public Map<String, Object> politica(String empresa) {
        return db.linha(com(USO, POLITICA) + """
                select coalesce(sum(req_ok) filter (where not conforme), 0) as req_fora from usop
                """, empresa);
    }

    public Map<String, Object> custo(String empresa) {
        return db.linha("""
                select coalesce(sum(${faturamento.custo}::numeric), 0) as custo from ${faturamento}
                where (:emp = 'ALL' or ${faturamento.empresa} = :emp)""", empresa);
    }

    public Map<String, Object> orcamento(String empresa) {
        return db.linha("""
                select coalesce(sum(${orcamentos.valor}::numeric), 0) as orcamento from ${orcamentos}
                where (:emp = 'ALL' or ${orcamentos.empresa} = :emp)""", empresa);
    }

    public Map<String, Object> licencas(String empresa) {
        return db.linha(com(RECENTE, LICENCAS) + """
                select coalesce(sum(ativas), 0) as lic_ativas, coalesce(sum(ociosas), 0) as lic_ociosas from lic
                """, empresa);
    }

    public Map<String, Object> treinamento(String empresa) {
        return db.linha("""
                select count(*) as trein_total,
                       count(*) filter (where ${treinamentos.status} in (:treinamento_concluido)) as trein_concluidos,
                       count(distinct ${treinamentos.funcionario})
                         filter (where ${treinamentos.status} in (:treinamento_concluido)) as trein_pessoas
                from ${treinamentos} where (:emp = 'ALL' or ${treinamentos.empresa} = :emp)""", empresa);
    }

    public Map<String, Object> seguranca(String empresa) {
        return db.linha("""
                select count(*) as eventos,
                       count(*) filter (where ${eventos_seguranca.severidade} in (:seguranca_grave)) as eventos_graves
                from ${eventos_seguranca} where (:emp = 'ALL' or ${eventos_seguranca.empresa} = :emp)""", empresa);
    }
}
