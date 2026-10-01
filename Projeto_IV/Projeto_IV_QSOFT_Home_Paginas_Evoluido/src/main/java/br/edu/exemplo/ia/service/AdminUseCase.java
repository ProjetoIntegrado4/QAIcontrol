package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;

import java.util.List;
import java.util.UUID;

public interface AdminUseCase {
    AdminResponse create(AdminRequest request);

    List<AdminResponse> list(UUID empresaId);

    AdminResponse getById(UUID id);
}