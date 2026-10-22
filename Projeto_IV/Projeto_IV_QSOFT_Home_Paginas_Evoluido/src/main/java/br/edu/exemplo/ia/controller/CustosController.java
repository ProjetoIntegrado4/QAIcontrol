package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.CustoOpcoesResponse;
import br.edu.exemplo.ia.dto.CustoRequest;
import br.edu.exemplo.ia.dto.CustoResponse;
import br.edu.exemplo.ia.service.custos.CustosUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custos de IA (US PP-18, tela frontend/public/custos.html). Os parâmetros chegam como texto e são
 * validados no caso de uso; erros de validação viram 400 com mensagem pelo ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/custos")
public class CustosController {
    private final CustosUseCase useCase;

    public CustosController(CustosUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/opcoes")
    public CustoOpcoesResponse opcoes(@RequestParam(required = false) String empresaId) {
        return useCase.opcoes(empresaId);
    }

    /** inicio e fim em AAAA-MM-DD (fim inclusive); departamentoId e colaboradorId são opcionais e exclusivos. */
    @GetMapping
    public CustoResponse calcular(@RequestParam(required = false) String empresaId,
                                  @RequestParam(required = false) String inicio,
                                  @RequestParam(required = false) String fim,
                                  @RequestParam(required = false) String departamentoId,
                                  @RequestParam(required = false) String colaboradorId) {
        return useCase.calcular(new CustoRequest(empresaId, inicio, fim, departamentoId, colaboradorId));
    }
}
