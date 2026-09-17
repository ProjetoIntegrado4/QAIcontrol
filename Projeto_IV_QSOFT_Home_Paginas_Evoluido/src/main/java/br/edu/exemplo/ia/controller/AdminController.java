package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.AdminRequest;
import br.edu.exemplo.ia.dto.AdminResponse;
import br.edu.exemplo.ia.service.AdminUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminUseCase useCase;

    public AdminController(AdminUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public AdminResponse create(@Valid @RequestBody AdminRequest request) {
        return useCase.create(request);
    }

    @GetMapping
    public List<AdminResponse> list(@RequestParam(required = false) UUID empresaId) {
        return useCase.list(empresaId);
    }

    @GetMapping("/{id}")
    public AdminResponse getById(@PathVariable UUID id) {
        return useCase.getById(id);
    }
}