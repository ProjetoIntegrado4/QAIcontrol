package br.edu.exemplo.ia.dto;

import java.math.BigDecimal;

/** Custo de um departamento, colaborador ou ferramenta no período. */
public record CustoItemResponse(
        String id,
        String nome,
        BigDecimal custo,
        long usos,
        long usosSemPreco
) {
}
