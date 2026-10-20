package br.edu.exemplo.ia.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CertificacaoResponse(
        UUID id,
        UUID funcionarioId,
        String funcionarioNome,
        String nome,
        String instituicao,
        LocalDate dataConclusao,
        String tipo,
        int pontos,
        Instant dataCriacao
) {
}
