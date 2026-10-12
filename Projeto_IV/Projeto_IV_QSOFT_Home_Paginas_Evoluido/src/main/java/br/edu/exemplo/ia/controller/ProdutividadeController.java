package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.service.ProdutividadeUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

/** Dados do painel de produtividade de IA (frontend/public/produtividade.html). */
@RestController
@RequestMapping("/api/produtividade")
public class ProdutividadeController {
    private final ProdutividadeUseCase useCase;

    public ProdutividadeController(ProdutividadeUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        return useCase.resumo();
    }

    /** empresa: id da empresa (ex.: C01) ou ALL para o consolidado. */
    @GetMapping("/empresas/{empresa}")
    public Map<String, Object> painel(@PathVariable String empresa) {
        return useCase.painel(empresa);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> naoEncontrada(NoSuchElementException e) {
        return Map.of("message", e.getMessage());
    }
}
