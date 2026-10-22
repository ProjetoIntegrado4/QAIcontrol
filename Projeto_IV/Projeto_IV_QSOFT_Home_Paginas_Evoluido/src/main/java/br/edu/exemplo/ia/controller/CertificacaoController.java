package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.CertificacaoRequest;
import br.edu.exemplo.ia.dto.CertificacaoResponse;
import br.edu.exemplo.ia.dto.ResumoCertificacaoResponse;
import br.edu.exemplo.ia.service.CertificacaoUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/certificacao")
public class CertificacaoController {
    private final CertificacaoUseCase useCase;

    public CertificacaoController(CertificacaoUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public CertificacaoResponse registrar(@RequestBody CertificacaoRequest request) {
        return useCase.registrar(request);
    }

    @GetMapping
    public List<CertificacaoResponse> listar(@RequestParam UUID funcionarioId) {
        return useCase.listar(funcionarioId);
    }

    @GetMapping("/resumo")
    public ResumoCertificacaoResponse resumo(@RequestParam UUID funcionarioId) {
        return useCase.resumo(funcionarioId);
    }
}
