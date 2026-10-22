package br.edu.exemplo.ia.repository.custos;

/** Dimensão pela qual o uso é somado no banco. Cada uma vira a coluna "id" do resultado. */
public enum Agrupamento {
    TOTAL("'total'"),
    DEPARTAMENTO("u.${uso_custo.departamento}"),
    COLABORADOR("u.${uso_custo.funcionario}"),
    FERRAMENTA("u.${uso_custo.ferramenta}");

    final String coluna;

    Agrupamento(String coluna) {
        this.coluna = coluna;
    }
}
