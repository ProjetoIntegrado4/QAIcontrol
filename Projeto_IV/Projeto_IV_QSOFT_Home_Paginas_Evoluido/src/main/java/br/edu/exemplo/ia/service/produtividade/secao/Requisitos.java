package br.edu.exemplo.ia.service.produtividade.secao;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Colunas lógicas lidas por cada trecho de consulta (ver produtividade.yml).
 * As seções combinam estes conjuntos para declarar do que precisam.
 */
public final class Requisitos {

    private Requisitos() {
    }

    public static final Set<String> USO = Set.of("uso.funcionario", "uso.empresa", "uso.departamento", "uso.ferramenta",
            "uso.data", "uso.requisicoes", "uso.status", "uso.tokens_entrada", "uso.tokens_saida", "uso.latencia");
    public static final Set<String> ATIVOS = Set.of("funcionarios.id", "funcionarios.empresa",
            "funcionarios.departamento", "funcionarios.ativo");
    public static final Set<String> FUNCIONARIOS = Set.of("funcionarios.id", "funcionarios.departamento",
            "funcionarios.cargo", "funcionarios.senioridade");
    public static final Set<String> DEPARTAMENTOS = Set.of("departamentos.id", "departamentos.nome");
    public static final Set<String> EMPRESAS = Set.of("empresas.id", "empresas.nome", "empresas.setor", "empresas.porte");
    public static final Set<String> POLITICA = Set.of("ferramentas_aprovadas.empresa", "ferramentas_aprovadas.ferramenta",
            "ferramentas_aprovadas.status", "ferramentas_aprovadas.areas", "departamentos.id", "departamentos.nome");
    public static final Set<String> LICENCAS = Set.of("licencas.empresa", "licencas.funcionario",
            "licencas.ferramenta", "licencas.status");
    public static final Set<String> FERRAMENTAS = Set.of("ferramentas.id", "ferramentas.nome", "ferramentas.categoria");
    public static final Set<String> CUSTO = Set.of("faturamento.empresa", "faturamento.custo");
    public static final Set<String> CUSTO_MENSAL = Set.of("faturamento.empresa", "faturamento.custo", "faturamento.mes");
    public static final Set<String> CUSTO_FERRAMENTA = Set.of("faturamento.empresa", "faturamento.custo",
            "faturamento.ferramenta");
    public static final Set<String> CUSTO_AREA = Set.of("faturamento.empresa", "faturamento.custo",
            "faturamento.centro_custo", "departamentos.id", "departamentos.nome");
    public static final Set<String> ORCAMENTO = Set.of("orcamentos.empresa", "orcamentos.valor");
    public static final Set<String> ORCAMENTO_MENSAL = Set.of("orcamentos.empresa", "orcamentos.valor",
            "orcamentos.ano", "orcamentos.mes");
    public static final Set<String> TREINAMENTOS = Set.of("treinamentos.empresa", "treinamentos.funcionario",
            "treinamentos.status");
    public static final Set<String> SEGURANCA = Set.of("eventos_seguranca.empresa", "eventos_seguranca.tipo",
            "eventos_seguranca.severidade");
    public static final Set<String> PULL_REQUESTS = Set.of("projetos.id", "projetos.empresa", "pull_requests.projeto",
            "pull_requests.funcionario", "pull_requests.criado_em", "pull_requests.mergeado_em", "pull_requests.status");
    public static final Set<String> BUILDS = Set.of("builds.projeto", "builds.data", "builds.status");
    public static final Set<String> DEPLOYS = Set.of("deploys.projeto", "deploys.data", "deploys.status");

    /** União de conjuntos de requisitos, na ordem em que aparecem. */
    @SafeVarargs
    public static Set<String> de(Set<String>... conjuntos) {
        Set<String> r = new LinkedHashSet<>();
        for (Set<String> c : conjuntos) r.addAll(c);
        return r;
    }
}
