package br.edu.exemplo.ia.service.produtividade.importacao;

import java.util.regex.Pattern;

/**
 * Tipo PostgreSQL de uma coluna de CSV, deduzido dos valores. A dedução vai do tipo mais estreito
 * para o mais largo: um valor que não cabe no tipo atual alarga a coluna (inteiro -> numérico,
 * data -> data e hora, qualquer conflito -> texto).
 */
enum TipoColuna {
    INDEFINIDO(null),
    BOOLEANO("boolean"),
    INTEIRO("bigint"),
    NUMERICO("numeric"),
    DATA("date"),
    DATA_HORA("timestamp"),
    TEXTO("text");

    private static final Pattern BOOL = Pattern.compile("(?i)true|false");
    private static final Pattern INT = Pattern.compile("-?\\d{1,18}");
    private static final Pattern NUM = Pattern.compile("-?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TS = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?");

    final String sql;

    TipoColuna(String sql) {
        this.sql = sql;
    }

    /** Tipo de um único valor não vazio. */
    static TipoColuna de(String valor) {
        if (BOOL.matcher(valor).matches()) return BOOLEANO;
        if (INT.matcher(valor).matches()) return INTEIRO;
        if (NUM.matcher(valor).matches()) return NUMERICO;
        if (DATE.matcher(valor).matches()) return DATA;
        if (TS.matcher(valor).matches()) return DATA_HORA;
        return TEXTO;
    }

    /** Menor tipo que comporta este e o outro. */
    TipoColuna combinar(TipoColuna outro) {
        if (this == outro || outro == INDEFINIDO) return this;
        if (this == INDEFINIDO) return outro;
        if (numerico(this) && numerico(outro)) return NUMERICO;
        if (temporal(this) && temporal(outro)) return DATA_HORA;
        return TEXTO;
    }

    /** Tipo SQL final; colunas sem nenhum valor ficam como texto. */
    String sql() {
        return this == INDEFINIDO ? TEXTO.sql : sql;
    }

    private static boolean numerico(TipoColuna t) {
        return t == INTEIRO || t == NUMERICO;
    }

    private static boolean temporal(TipoColuna t) {
        return t == DATA || t == DATA_HORA;
    }
}
