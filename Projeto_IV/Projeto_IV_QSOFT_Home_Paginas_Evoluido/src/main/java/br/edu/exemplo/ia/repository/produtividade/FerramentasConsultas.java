package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.LICENCAS;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.POLITICA;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.RECENTE;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.USO;
import static br.edu.exemplo.ia.repository.produtividade.Fragmentos.com;

/** Indicadores por ferramenta de IA. Todas as listas usam a coluna "id" com o id da ferramenta. */
@Repository
public class FerramentasConsultas {

    private final ConsultaPainel db;

    public FerramentasConsultas(ConsultaPainel db) {
        this.db = db;
    }

    /** Requisições bem-sucedidas e usuários de cada ferramenta usada pela empresa. */
    public List<Map<String, Object>> uso(String empresa) {
        return db.lista(com(USO) + """
                select ferramenta as id, sum(rc) as req, count(distinct funcionario) as usuarios
                from uso where ok group by ferramenta order by req desc
                """, empresa);
    }

    /** Nome e categoria de todas as ferramentas cadastradas. */
    public List<Map<String, Object>> cadastro() {
        return db.lista("""
                select ${ferramentas.id} as id, ${ferramentas.nome} as nome, ${ferramentas.categoria} as categoria
                from ${ferramentas}""", "ALL");
    }

    public List<Map<String, Object>> custo(String empresa) {
        return db.lista("""
                select ${faturamento.ferramenta} as id, sum(${faturamento.custo}::numeric) as custo from ${faturamento}
                where (:emp = 'ALL' or ${faturamento.empresa} = :emp) group by 1""", empresa);
    }

    public List<Map<String, Object>> licencas(String empresa) {
        return db.lista(com(RECENTE, LICENCAS) + """
                select ferramenta as id, ativas as lic_ativas, ociosas as lic_ociosas from lic
                """, empresa);
    }

    /** Requisições bem-sucedidas fora da política, por ferramenta. */
    public List<Map<String, Object>> foraDaPolitica(String empresa) {
        return db.lista(com(USO, POLITICA) + """
                select ferramenta as id, coalesce(sum(req_ok) filter (where not conforme), 0) as req_fora
                from usop group by ferramenta
                """, empresa);
    }
}
