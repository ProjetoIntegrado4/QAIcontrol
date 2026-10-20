package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** Dados gerais do painel: empresas, período da base e avisos de qualidade dos dados. */
@Repository
public class ResumoConsultas {

    private static final String TODAS = "ALL";
    private final ConsultaPainel db;

    public ResumoConsultas(ConsultaPainel db) {
        this.db = db;
    }

    public List<Map<String, Object>> empresas() {
        return db.lista("""
                select ${empresas.id} as id, ${empresas.nome} as nome, ${empresas.setor} as setor, ${empresas.porte} as porte
                from ${empresas} order by 1""", TODAS);
    }

    public Map<String, Object> periodo() {
        return db.linha("""
                select to_char(min(${uso.data}::timestamp), 'YYYY-MM') as inicio,
                       to_char(max(${uso.data}::timestamp), 'YYYY-MM') as fim,
                       count(*) as linhas_uso
                from ${uso}""", TODAS);
    }

    public Long usoSemCentroDeCusto() {
        return db.valor("select count(*) from ${uso} where ${uso.centro_custo} is null", TODAS, Long.class);
    }

    public Map<String, Object> pullRequestsInconsistentes() {
        return db.linha("""
                select count(*) filter (where ${pull_requests.mergeado_em}::timestamp < ${pull_requests.criado_em}::timestamp)
                         as lead_negativo,
                       count(*) filter (where ${pull_requests.revisoes}::numeric <> trunc(${pull_requests.revisoes}::numeric))
                         as review_imputado
                from ${pull_requests}""", TODAS);
    }

    public Long funcionariosSemCentroDeCusto() {
        return db.valor("select count(*) from ${funcionarios} where ${funcionarios.centro_custo} is null", TODAS, Long.class);
    }
}
