package br.edu.exemplo.ia.service.produtividade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conversões e montagem de mapas usadas pelas seções do painel. */
public final class Valores {

    private Valores() {
    }

    /** Número inteiro de uma coluna (nulo vira 0). */
    public static long inteiro(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }

    /** Arredonda para {@code casas} decimais; nulo e NaN viram nulo. */
    public static Double arredondar(Object v, int casas) {
        if (v == null) return null;
        double d = ((Number) v).doubleValue();
        if (Double.isNaN(d)) return null;
        return BigDecimal.valueOf(d).setScale(casas, RoundingMode.HALF_EVEN).doubleValue();
    }

    /** Percentual de a sobre b, ou nulo quando b é zero. */
    public static Double percentual(long a, long b, int casas) {
        return b > 0 ? arredondar(100.0 * a / b, casas) : null;
    }

    /** Mapa ordenado a partir de pares chave, valor. */
    public static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Linhas "chave, total" viram um mapa chave -> total, na ordem da consulta. */
    public static Map<String, Object> contagem(List<Map<String, Object>> linhas) {
        Map<String, Object> m = new LinkedHashMap<>();
        linhas.forEach(l -> m.put(String.valueOf(l.get("chave")), inteiro(l.get("total"))));
        return m;
    }

    /**
     * Acrescenta às linhas da base as colunas de outra consulta, casando pela coluna {@code chave}.
     * Linhas sem correspondência recebem os valores de {@code padrao}.
     */
    public static List<Map<String, Object>> juntar(List<Map<String, Object>> base, String chave,
                                                   List<Map<String, Object>> extra, Map<String, Object> padrao) {
        Map<Object, Map<String, Object>> porChave = new LinkedHashMap<>();
        extra.forEach(l -> porChave.put(l.get(chave), l));
        List<Map<String, Object>> r = new ArrayList<>(base.size());
        for (Map<String, Object> linha : base) {
            Map<String, Object> m = new LinkedHashMap<>(linha);
            Map<String, Object> e = porChave.get(linha.get(chave));
            padrao.forEach((k, v) -> m.put(k, e != null && e.get(k) != null ? e.get(k) : v));
            r.add(m);
        }
        return r;
    }
}
