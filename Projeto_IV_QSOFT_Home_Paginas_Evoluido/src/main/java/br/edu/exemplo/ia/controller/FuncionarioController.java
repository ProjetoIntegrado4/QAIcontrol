package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.FuncionarioRequest;
import br.edu.exemplo.ia.dto.FuncionarioResponse;
import br.edu.exemplo.ia.service.FuncionarioUseCase;
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
@RequestMapping("/api/funcionario")
public class FuncionarioController {
    private final FuncionarioUseCase useCase;

    public FuncionarioController(FuncionarioUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public FuncionarioResponse create(@Valid @RequestBody FuncionarioRequest request) {
        return useCase.create(request);
    }

    @GetMapping
    public List<FuncionarioResponse> list(@RequestParam(required = false) UUID empresaId) {
        return useCase.list(empresaId);
    }

    @GetMapping("/{id}")
    public FuncionarioResponse getById(@PathVariable UUID id) {
        return useCase.getById(id);
    }
}