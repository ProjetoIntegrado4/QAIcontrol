package br.edu.exemplo.ia.dto;

import java.util.UUID;

/** Aceite do colaborador. {@code politicaId} é o id da VERSÃO aceita (o {@code id} de PoliticaResponse). */
public record AceiteRequest(
        UUID funcionarioId,
        UUID politicaId
) {
}
