package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.CertificacaoRequest;
import br.edu.exemplo.ia.dto.CertificacaoResponse;
import br.edu.exemplo.ia.dto.ResumoCertificacaoResponse;

import java.util.List;
import java.util.UUID;

public interface CertificacaoUseCase {
    CertificacaoResponse registrar(CertificacaoRequest request);

    List<CertificacaoResponse> listar(UUID funcionarioId);

    ResumoCertificacaoResponse resumo(UUID funcionarioId);
}
