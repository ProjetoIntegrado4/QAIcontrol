package br.edu.exemplo.ia.service.produtividade.importacao;

import org.junit.jupiter.api.Test;

import static br.edu.exemplo.ia.service.produtividade.importacao.TipoColuna.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoColunaTest {

    @Test
    void deveDeduzirOTipoDeCadaValor() {
        assertEquals(BOOLEANO, TipoColuna.de("TRUE"));
        assertEquals(INTEIRO, TipoColuna.de("-12"));
        assertEquals(NUMERICO, TipoColuna.de("3.5e2"));
        assertEquals(NUMERICO, TipoColuna.de("12345678901234567890"));
        assertEquals(DATA, TipoColuna.de("2025-01-31"));
        assertEquals(DATA_HORA, TipoColuna.de("2025-01-31 10:00:00.5"));
        assertEquals(TEXTO, TipoColuna.de("C01"));
    }

    @Test
    void combinarDeveAlargarOTipo() {
        assertEquals(INTEIRO, INDEFINIDO.combinar(INTEIRO));
        assertEquals(INTEIRO, INTEIRO.combinar(INDEFINIDO));
        assertEquals(NUMERICO, INTEIRO.combinar(NUMERICO));
        assertEquals(DATA_HORA, DATA.combinar(DATA_HORA));
        assertEquals(TEXTO, INTEIRO.combinar(DATA));
        assertEquals(TEXTO, BOOLEANO.combinar(INTEIRO));
    }

    @Test
    void colunaSemValoresViraTexto() {
        assertEquals("text", INDEFINIDO.sql());
        assertEquals("bigint", INTEIRO.sql());
    }
}
