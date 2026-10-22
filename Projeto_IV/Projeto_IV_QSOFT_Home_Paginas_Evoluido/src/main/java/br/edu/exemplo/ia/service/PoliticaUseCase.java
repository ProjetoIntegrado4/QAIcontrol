package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.AceiteResponse;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;

import java.util.List;
import java.util.UUID;

public interface PoliticaUseCase {
    /** Publica a versão 1 de uma política da empresa (só administrador). */
    PoliticaResponse publicar(PoliticaRequest request);

    /** Publica a próxima versão da política; a anterior deixa de ser vigente, mas continua guardada (RN14). */
    PoliticaResponse publicarNovaVersao(UUID codigo, PoliticaRequest request);

    List<PoliticaResponse> listarVigentes(UUID empresaId);

    List<PoliticaResponse> listarVersoes(UUID codigo);

    /** Políticas vigentes e obrigatórias que o colaborador ainda não aceitou. */
    List<PoliticaResponse> listarPendentes(UUID empresaId, UUID funcionarioId);

    AceiteResponse registrarAceite(AceiteRequest request);

    List<AceiteResponse> listarAceites(UUID funcionarioId);
}
