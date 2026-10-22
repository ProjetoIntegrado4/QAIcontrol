package br.edu.exemplo.ia.dto;

/** Item de um select da tela de custos. {@code departamentoId} só vem preenchido para colaboradores. */
public record OpcaoCustoResponse(
        String id,
        String nome,
        String departamentoId
) {
}
