package br.edu.exemplo.ia.dto;

import java.time.Instant;
import java.util.UUID;

/** Comprovante do aceite (RN15): quem aceitou, qual versão e quando. */
public record AceiteResponse(
        UUID id,
        UUID funcionarioId,
        String funcionarioNome,
        UUID politicaId,
        UUID codigo,
        String titulo,
        int versao,
        Instant dataAceite
) {
}
