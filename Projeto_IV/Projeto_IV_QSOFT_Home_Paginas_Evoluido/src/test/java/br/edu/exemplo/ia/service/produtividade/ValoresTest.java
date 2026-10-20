package br.edu.exemplo.ia.service.produtividade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValoresTest {

    @Test
    void conversoesNumericas() {
        assertEquals(0, Valores.inteiro(null));
        assertEquals(7, Valores.inteiro(new BigDecimal("7.9")));
        assertNull(Valores.arredondar(null, 1));
        assertNull(Valores.arredondar(Double.NaN, 1));
        assertEquals(1.24, Valores.arredondar(1.235, 2));
        assertEquals(33.3, Valores.percentual(1, 3, 1));
        assertNull(Valores.percentual(1, 0, 1));
    }

    @Test
    void contagemEJuncao() {
        assertEquals(Map.of("A", 2L), Valores.contagem(List.of(mapa("chave", "A", "total", 2))));

        List<Map<String, Object>> r = Valores.juntar(
                List.of(mapa("k", 1, "x", "a"), mapa("k", 2, "x", "b")), "k",
                List.of(mapa("k", 1, "y", 9), mapa("k", 3, "y", 8)), mapa("y", 0));
        assertEquals(mapa("k", 1, "x", "a", "y", 9), r.get(0));
        assertEquals(mapa("k", 2, "x", "b", "y", 0), r.get(1));
    }
}
