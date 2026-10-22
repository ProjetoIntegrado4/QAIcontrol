package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regra de pontuação de AI Literacy por capacitação registrada (US PP-19).
 * É o único lugar do código com essa regra; os valores ficam no application.yml (app.ai-literacy).
 *
 * PRELIMINAR – validar com o PO: os pesos ainda não estão definidos no documento do projeto (RF10/RF11).
 * O cálculo completo do indicador de AI Literacy é outra task (PS-283).
 */
@Component
public class PontuacaoAiLiteracy {
    private final int pontosCurso;
    private final int pontosCertificacao;

    public PontuacaoAiLiteracy(
            @Value("${app.ai-literacy.pontos-curso}") int pontosCurso,
            @Value("${app.ai-literacy.pontos-certificacao}") int pontosCertificacao
    ) {
        this.pontosCurso = pontosCurso;
        this.pontosCertificacao = pontosCertificacao;
    }

    public int pontosPor(TipoCertificacao tipo) {
        if (tipo == TipoCertificacao.CERTIFICACAO) {
            return pontosCertificacao;
        }
        return pontosCurso;
    }

    public int somar(List<TipoCertificacao> tipos) {
        int total = 0;
        for (TipoCertificacao tipo : tipos) {
            total += pontosPor(tipo);
        }
        return total;
    }
}
