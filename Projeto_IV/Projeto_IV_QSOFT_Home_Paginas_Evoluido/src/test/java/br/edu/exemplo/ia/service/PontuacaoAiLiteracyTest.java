package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PontuacaoAiLiteracyTest {

    private final PontuacaoAiLiteracy pontuacao = new PontuacaoAiLiteracy(10, 20);

    @Test
    void devePontuarDeAcordoComOTipo() {
        assertEquals(10, pontuacao.pontosPor(TipoCertificacao.CURSO));
        assertEquals(20, pontuacao.pontosPor(TipoCertificacao.CERTIFICACAO));
    }

    @Test
    void deveSomarOsPontosDeVariosRegistros() {
        List<TipoCertificacao> tipos = List.of(
                TipoCertificacao.CURSO, TipoCertificacao.CERTIFICACAO, TipoCertificacao.CERTIFICACAO);

        assertEquals(50, pontuacao.somar(tipos));
        assertEquals(0, pontuacao.somar(List.of()));
    }
}
