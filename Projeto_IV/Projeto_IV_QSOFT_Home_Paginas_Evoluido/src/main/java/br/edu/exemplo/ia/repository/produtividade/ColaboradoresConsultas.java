package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.USO;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.com;

/** Ranking dos colaboradores com mais requisições bem-sucedidas na janela recente da base. */
@Repository
public class ColaboradoresConsultas {

    private final ConsultaPainel db;

    public ColaboradoresConsultas(ConsultaPainel db) {
        this.db = db;
    }

    public List<Map<String, Object>> maisAtivos(String empresa) {
        return db.lista(com(USO) + """
                , fim as (select max(${uso.data}::timestamp) as fim from ${uso}),
                r as (select funcionario, sum(rc) as req, count(distinct ferramenta) as ferramentas,
                             count(distinct ts::date) as dias
                      from uso, fim where ok and ts >= fim.fim - make_interval(days => :janela_dias)
                      group by funcionario)
                select r.funcionario as id, e.${funcionarios.cargo} as cargo, e.${funcionarios.senioridade} as senioridade,
                       d.${departamentos.nome} as area, r.req, r.ferramentas, r.dias
                from r
                left join ${funcionarios} e on e.${funcionarios.id} = r.funcionario
                left join ${departamentos} d on d.${departamentos.id} = e.${funcionarios.departamento}
                order by r.req desc, r.funcionario limit :limite_ranking
                """, empresa);
    }
}
