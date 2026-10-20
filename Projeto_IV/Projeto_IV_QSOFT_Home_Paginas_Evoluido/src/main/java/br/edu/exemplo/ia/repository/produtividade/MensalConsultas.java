package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.MESES;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.USO;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.com;

/** Séries mensais: uso, custo faturado e orçamento, cada uma de uma tabela. */
@Repository
public class MensalConsultas {

    private final ConsultaPainel db;

    public MensalConsultas(ConsultaPainel db) {
        this.db = db;
    }

    /** Um registro por mês da base, inclusive meses sem uso da empresa. */
    public List<Map<String, Object>> uso(String empresa) {
        return db.lista(com(USO, MESES) + """
                , um as (select mes, sum(rc) filter (where ok) as req, count(distinct funcionario) filter (where ok) as ativos,
                                sum(rc) as total
                         from uso group by mes)
                select m.mes, coalesce(um.req, 0) as req, coalesce(um.ativos, 0) as ativos,
                       case when um.total > 0 then 100.0 * um.req / um.total end as sucesso
                from meses m left join um on um.mes = m.mes
                order by m.mes
                """, empresa);
    }

    public List<Map<String, Object>> custo(String empresa) {
        return db.lista("""
                select left(${faturamento.mes}::text, 7) as mes, sum(${faturamento.custo}::numeric) as custo
                from ${faturamento} where (:emp = 'ALL' or ${faturamento.empresa} = :emp) group by 1""", empresa);
    }

    public List<Map<String, Object>> orcamento(String empresa) {
        return db.lista("""
                select ${orcamentos.ano}::int || '-' || lpad(${orcamentos.mes}::int::text, 2, '0') as mes,
                       sum(${orcamentos.valor}::numeric) as orcamento
                from ${orcamentos} where (:emp = 'ALL' or ${orcamentos.empresa} = :emp) group by 1""", empresa);
    }
}
