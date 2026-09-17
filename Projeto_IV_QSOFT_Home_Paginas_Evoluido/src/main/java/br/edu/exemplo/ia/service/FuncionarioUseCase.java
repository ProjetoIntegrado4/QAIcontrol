package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.FuncionarioRequest;
import br.edu.exemplo.ia.dto.FuncionarioResponse;

import java.util.List;
import java.util.UUID;

public interface FuncionarioUseCase {
    FuncionarioResponse create(FuncionarioRequest request);

    List<FuncionarioResponse> list(UUID empresaId);

    FuncionarioResponse getById(UUID id);
}