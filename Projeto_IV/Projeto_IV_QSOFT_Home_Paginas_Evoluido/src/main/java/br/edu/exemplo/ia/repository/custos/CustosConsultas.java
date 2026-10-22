package br.edu.exemplo.ia.repository.custos;

import br.edu.exemplo.ia.repository.produtividade.EsquemaBase;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Consultas do cálculo de custos de IA (US PP-18) sobre as tabelas importadas dos CSVs.
 * Usam os nomes lógicos de produtividade.yml (resolvidos pelo {@link EsquemaBase}) e somam o uso no banco:
 * o resultado tem uma linha por dimensão × modelo × agente, nunca os registros de uso um a um.
 * O preço é aplicado depois, pela RegraCusto.
 */
@Repository
public class CustosConsultas {

    private static final String TODAS = "ALL";

    private final NamedParameterJdbcTemplate jdbc;
    private final EsquemaBase esquema;

    public CustosConsultas(NamedParameterJdbcTemplate jdbc, EsquemaBase esquema) {
        this.jdbc = jdbc;
        this.esquema = esquema;
    }

    /**
     * Usos cobrados no período, somados por dimensão, modelo e agente/uso direto.
     * Colunas: id, modelo, agente, usos, tokens_entrada, tokens_saida.
     */
    public List<Map<String, Object>> uso(FiltroCusto filtro, Agrupamento agrupamento) {
        return jdbc.queryForList(esquema.sql("""
                select %s as id, u.${uso_custo.modelo} as modelo,
                       coalesce(u.${uso_custo.aplicacao} in (:aplicacoes_agente), false) as agente,
                       count(*) as usos,
                       coalesce(sum(u.${uso_custo.tokens_entrada}::numeric), 0) as tokens_entrada,
                       coalesce(sum(u.${uso_custo.tokens_saida}::numeric), 0) as tokens_saida
                from ${uso_custo} u
                where u.${uso_custo.empresa} = :emp
                  and u.${uso_custo.data}::timestamp >= :inicio and u.${uso_custo.data}::timestamp < :fim_exclusivo
                  and u.${uso_custo.status} in (:status_cobrados)
                  and (cast(:departamento as text) is null or u.${uso_custo.departamento} = :departamento)
                  and (cast(:colaborador as text) is null or u.${uso_custo.funcionario} = :colaborador)
                group by 1, 2, 3""".formatted(agrupamento.coluna)), parametros(filtro));
    }

    /** Preço por 1k tokens de cada modelo. Colunas: id, preco_entrada, preco_saida. */
    public List<Map<String, Object>> precos() {
        return lista("""
                select ${modelos.id} as id, ${modelos.preco_entrada}::numeric as preco_entrada,
                       ${modelos.preco_saida}::numeric as preco_saida
                from ${modelos}""", TODAS);
    }

    public List<Map<String, Object>> empresas() {
        return lista("select ${empresas.id} as id, ${empresas.nome} as nome from ${empresas} order by 1", TODAS);
    }

    public List<Map<String, Object>> departamentos(String empresa) {
        return lista("""
                select ${departamentos.id} as id, ${departamentos.nome} as nome from ${departamentos}
                where ${departamentos.empresa} = :emp order by 1""", empresa);
    }

    /** Colaboradores da empresa. Colunas: id, cargo, departamento. */
    public List<Map<String, Object>> colaboradores(String empresa) {
        return lista("""
                select ${funcionarios.id} as id, ${funcionarios.cargo} as cargo,
                       ${funcionarios.departamento} as departamento
                from ${funcionarios} where ${funcionarios.empresa} = :emp order by 1""", empresa);
    }

    public List<Map<String, Object>> ferramentas() {
        return lista("select ${ferramentas.id} as id, ${ferramentas.nome} as nome from ${ferramentas}", TODAS);
    }

    /** Primeiro e último dia com uso registrado (AAAA-MM-DD). Colunas: inicio, fim. */
    public Map<String, Object> periodo() {
        return jdbc.queryForMap(esquema.sql("""
                select to_char(min(${uso_custo.data}::timestamp), 'YYYY-MM-DD') as inicio,
                       to_char(max(${uso_custo.data}::timestamp), 'YYYY-MM-DD') as fim
                from ${uso_custo}"""), esquema.parametros(TODAS));
    }

    public boolean empresaExiste(String empresa) {
        return existe("select exists (select 1 from ${empresas} where ${empresas.id} = :emp)",
                esquema.parametros(empresa));
    }

    public boolean departamentoDaEmpresa(String empresa, String departamento) {
        return existe("""
                select exists (select 1 from ${departamentos}
                               where ${departamentos.id} = :departamento and ${departamentos.empresa} = :emp)""",
                esquema.parametros(empresa).addValue("departamento", departamento));
    }

    public boolean colaboradorDaEmpresa(String empresa, String colaborador) {
        return existe("""
                select exists (select 1 from ${funcionarios}
                               where ${funcionarios.id} = :colaborador and ${funcionarios.empresa} = :emp)""",
                esquema.parametros(empresa).addValue("colaborador", colaborador));
    }

    private List<Map<String, Object>> lista(String modelo, String empresa) {
        return jdbc.queryForList(esquema.sql(modelo), esquema.parametros(empresa));
    }

    private boolean existe(String modelo, MapSqlParameterSource parametros) {
        return Boolean.TRUE.equals(jdbc.queryForObject(esquema.sql(modelo), parametros, Boolean.class));
    }

    private MapSqlParameterSource parametros(FiltroCusto f) {
        return esquema.parametros(f.empresa())
                .addValue("inicio", f.inicio().atStartOfDay())
                .addValue("fim_exclusivo", f.fim().plusDays(1).atStartOfDay())
                .addValue("departamento", f.departamento(), Types.VARCHAR)
                .addValue("colaborador", f.colaborador(), Types.VARCHAR)
                .addValue("status_cobrados", naoVazia(f.statusCobrados()))
                .addValue("aplicacoes_agente", naoVazia(f.aplicacoesAgente()));
    }

    /** "in ()" é SQL inválido: lista vazia vira [""], que não casa com nenhum valor real. */
    private static List<String> naoVazia(List<String> valores) {
        return valores.isEmpty() ? List.of("") : valores;
    }
}
