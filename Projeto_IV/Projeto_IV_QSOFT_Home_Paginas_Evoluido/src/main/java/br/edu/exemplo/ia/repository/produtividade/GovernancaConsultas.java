package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** Contagens de eventos de segurança e de treinamentos, no formato chave x total. */
@Repository
public class GovernancaConsultas {

    private final ConsultaPainel db;

    public GovernancaConsultas(ConsultaPainel db) {
        this.db = db;
    }

    public List<Map<String, Object>> eventosPorTipo(String empresa) {
        return db.lista("""
                select ${eventos_seguranca.tipo} as chave, count(*) as total from ${eventos_seguranca}
                where (:emp = 'ALL' or ${eventos_seguranca.empresa} = :emp) group by 1 order by 2 desc, 1""", empresa);
    }

    public List<Map<String, Object>> eventosPorSeveridade(String empresa) {
        return db.lista("""
                select ${eventos_seguranca.severidade} as chave, count(*) as total from ${eventos_seguranca}
                where (:emp = 'ALL' or ${eventos_seguranca.empresa} = :emp) group by 1 order by 2 desc, 1""", empresa);
    }

    public List<Map<String, Object>> treinamentosPorSituacao(String empresa) {
        return db.lista("""
                select ${treinamentos.status} as chave, count(*) as total from ${treinamentos}
                where (:emp = 'ALL' or ${treinamentos.empresa} = :emp) group by 1 order by 2 desc, 1""", empresa);
    }
}
