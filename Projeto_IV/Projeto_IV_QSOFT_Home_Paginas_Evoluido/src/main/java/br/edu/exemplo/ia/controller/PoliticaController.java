package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.AceiteResponse;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.service.PoliticaUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Políticas de uso responsável de IA e aceite dos colaboradores (US PP-21).
 * POST e PUT de política só para o perfil Administrador (RN04), regra aplicada no SecurityConfig.
 */
@RestController
@RequestMapping("/api/politicas")
public class PoliticaController {
    private final PoliticaUseCase useCase;

    public PoliticaController(PoliticaUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public PoliticaResponse publicar(@RequestBody PoliticaRequest request) {
        return useCase.publicar(request);
    }

    @PutMapping("/{codigo}")
    public PoliticaResponse novaVersao(@PathVariable UUID codigo, @RequestBody PoliticaRequest request) {
        return useCase.novaVersao(codigo, request);
    }

    @GetMapping
    public List<PoliticaResponse> vigentes(@RequestParam UUID empresaId) {
        return useCase.vigentes(empresaId);
    }

    @GetMapping("/{codigo}/versoes")
    public List<PoliticaResponse> versoes(@PathVariable UUID codigo) {
        return useCase.versoes(codigo);
    }

    @GetMapping("/pendentes")
    public List<PoliticaResponse> pendentes(@RequestParam UUID empresaId, @RequestParam UUID funcionarioId) {
        return useCase.pendentes(empresaId, funcionarioId);
    }

    @PostMapping("/aceites")
    public AceiteResponse aceitar(@RequestBody AceiteRequest request) {
        return useCase.aceitar(request);
    }

    @GetMapping("/aceites")
    public List<AceiteResponse> aceites(@RequestParam UUID funcionarioId) {
        return useCase.aceites(funcionarioId);
    }
}
