package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.ATIVOS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.USO;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.com;

/**
 * Indicadores por área, agrupados pelo nome do departamento
 * (no consolidado, a mesma área de empresas diferentes soma).
 */
@Repository
public class AreasConsultas {

    private final ConsultaPainel db;

    public AreasConsultas(ConsultaPainel db) {
        this.db = db;
    }

    /** Colaboradores ativos, usuários de IA e requisições de cada área. */
    public List<Map<String, Object>> uso(String empresa) {
        return db.lista(com(USO, ATIVOS) + """
                , ea as (select a.funcionario, d.${departamentos.nome} as area from ativos a
                         join ${departamentos} d on d.${departamentos.id} = a.departamento),
                ua as (select d.${departamentos.nome} as area, u.funcionario, u.rc from uso u
                       join ${departamentos} d on d.${departamentos.id} = u.departamento where u.ok),
                req as (select area, sum(rc) as req, count(distinct funcionario) as usuarios_uso from ua group by area),
                usu as (select ua.area, count(distinct ua.funcionario) as usuarios from ua
                        join ea on ea.funcionario = ua.funcionario and ea.area = ua.area group by ua.area),
                tot as (select area, count(*) as funcionarios from ea group by area)
                select tot.area, tot.funcionarios, coalesce(usu.usuarios, 0) as usuarios, coalesce(req.req, 0) as req,
                       case when req.usuarios_uso > 0 then req.req / req.usuarios_uso else 0 end as req_usuario
                from tot left join usu on usu.area = tot.area left join req on req.area = tot.area
                order by req desc, tot.area
                """, empresa);
    }

    /** Custo faturado por área, ligando o centro de custo ao departamento. */
    public List<Map<String, Object>> custo(String empresa) {
        return db.lista("""
                select d.${departamentos.nome} as area, sum(b.${faturamento.custo}::numeric) as custo
                from ${faturamento} b
                join ${departamentos} d
                  on d.${departamentos.id} = replace(b.${faturamento.centro_custo}, :centro_custo_prefixo, '')
                where (:emp = 'ALL' or b.${faturamento.empresa} = :emp) group by 1""", empresa);
    }

    /** Percentual de matrículas em treinamento concluídas por área. */
    public List<Map<String, Object>> treinamento(String empresa) {
        return db.lista("""
                select d.${departamentos.nome} as area,
                       100.0 * avg((t.${treinamentos.status} in (:treinamento_concluido))::int) as trein
                from ${treinamentos} t
                join ${funcionarios} e on e.${funcionarios.id} = t.${treinamentos.funcionario}
                join ${departamentos} d on d.${departamentos.id} = e.${funcionarios.departamento}
                where (:emp = 'ALL' or t.${treinamentos.empresa} = :emp) group by 1""", empresa);
    }
}
