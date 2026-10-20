package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Verifica, durante um cálculo do painel, se as tabelas e colunas de cada parte existem no banco,
 * e guarda o que faltou para informar na resposta ("indisponiveis"). Uma instância por requisição.
 */
public class Disponibilidade {

    private final CatalogoBanco catalogo;
    private final Map<String, String> faltas = new TreeMap<>();

    public Disponibilidade(CatalogoBanco catalogo) {
        this.catalogo = catalogo;
    }

    /** true quando o banco tem tudo de que a parte precisa; senão registra a falta e devolve false. */
    public boolean tem(String parte, Collection<String> requisitos) {
        List<String> faltam = catalogo.faltantes(requisitos);
        if (faltam.isEmpty()) return true;
        synchronized (faltas) {
            faltas.put(parte, "faltam no banco: " + String.join(", ", faltam));
        }
        return false;
    }

    public Map<String, String> faltas() {
        synchronized (faltas) {
            return new TreeMap<>(faltas);
        }
    }
}
