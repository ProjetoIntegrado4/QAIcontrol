package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.PoliticaResponse;

import java.util.List;

/**
 * Uso de IA bloqueado porque o colaborador não aceitou a versão vigente de uma política obrigatória (US PP-21, BDD 2).
 * O ApiExceptionHandler responde 409 com a mensagem e a lista das políticas pendentes.
 */
public class AceitePendenteException extends RuntimeException {
    private final List<PoliticaResponse> pendentes;

    public AceitePendenteException(List<PoliticaResponse> pendentes) {
        super("Aceite da política de uso de IA obrigatório");
        this.pendentes = List.copyOf(pendentes);
    }

    public List<PoliticaResponse> getPendentes() {
        return pendentes;
    }
}
