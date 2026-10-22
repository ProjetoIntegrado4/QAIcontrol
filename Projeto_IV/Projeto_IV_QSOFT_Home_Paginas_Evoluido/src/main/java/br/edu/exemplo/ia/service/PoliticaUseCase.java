package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.AceiteResponse;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;

import java.util.List;
import java.util.UUID;

public interface PoliticaUseCase {
    PoliticaResponse publicar(PoliticaRequest request);

    PoliticaResponse novaVersao(UUID codigo, PoliticaRequest request);

    List<PoliticaResponse> vigentes(UUID empresaId);

    List<PoliticaResponse> versoes(UUID codigo);

    List<PoliticaResponse> pendentes(UUID empresaId, UUID funcionarioId);

    AceiteResponse aceitar(AceiteRequest request);

    List<AceiteResponse> aceites(UUID funcionarioId);
}
