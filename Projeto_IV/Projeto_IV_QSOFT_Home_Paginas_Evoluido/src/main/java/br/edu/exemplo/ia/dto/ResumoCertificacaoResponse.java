package br.edu.exemplo.ia.dto;

import java.util.UUID;

/** Totais de capacitação de um funcionário, usados no AI Maturity Score e no cálculo de AI Literacy. */
public record ResumoCertificacaoResponse(
        UUID funcionarioId,
        int totalRegistros,
        int totalCursos,
        int totalCertificacoes,
        int pontosAiLiteracy
) {
}
