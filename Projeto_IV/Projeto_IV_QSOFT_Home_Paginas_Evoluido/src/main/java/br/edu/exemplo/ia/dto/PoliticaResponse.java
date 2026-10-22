package br.edu.exemplo.ia.dto;

import java.time.Instant;
import java.util.UUID;

/** Uma versão de política de uso de IA. {@code id} identifica a versão; {@code codigo}, a política. */
public record PoliticaResponse(
        UUID id,
        UUID codigo,
        UUID empresaId,
        String empresaNome,
        String titulo,
        String conteudo,
        int versao,
        boolean obrigatoria,
        boolean vigente,
        Instant publicadaEm
) {
}
