package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.RecomendacaoRiscoResponse;
import br.edu.exemplo.ia.dto.RegrasRiscoResponse;
import br.edu.exemplo.ia.dto.UnidadeRiscoRequest;
import br.edu.exemplo.ia.dto.UnidadeRiscoResponse;
import br.edu.exemplo.ia.service.risco.RecomendacaoRiscoUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recomendações por faixa de risco de dependência de IA (Task PS-290 / RF27, tela frontend/public/risco.html).
 * O score chega como texto e é validado no caso de uso; erros viram 400 com mensagem pelo ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/risco/recomendacoes")
public class RecomendacaoRiscoController {
    private final RecomendacaoRiscoUseCase useCase;

    public RecomendacaoRiscoController(RecomendacaoRiscoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/regras")
    public RegrasRiscoResponse regras() {
        return useCase.regras();
    }

    /** score de 0 a 100; sem score a resposta é "dados insuficientes", sem recomendação. */
    @GetMapping
    public RecomendacaoRiscoResponse recomendar(@RequestParam(required = false) String score) {
        return useCase.recomendar(score);
    }

    @PostMapping
    public List<UnidadeRiscoResponse> recomendarLote(@RequestBody List<UnidadeRiscoRequest> unidades) {
        return useCase.recomendarLote(unidades);
    }
}
