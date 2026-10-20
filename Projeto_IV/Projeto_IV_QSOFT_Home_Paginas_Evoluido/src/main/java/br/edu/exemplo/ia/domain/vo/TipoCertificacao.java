package br.edu.exemplo.ia.domain.vo;

import java.util.Locale;

/** Tipo da capacitação registrada pelo colaborador: curso concluído ou certificação obtida. */
public enum TipoCertificacao {
    CURSO,
    CERTIFICACAO;

    /** Converte o texto enviado pela tela (ex.: "curso" ou "CERTIFICACAO") para o enum. */
    public static TipoCertificacao converter(String valor) {
        if (valor != null) {
            String texto = valor.trim().toUpperCase(Locale.ROOT);
            for (TipoCertificacao tipo : values()) {
                if (tipo.name().equals(texto)) {
                    return tipo;
                }
            }
        }
        throw new IllegalArgumentException("Tipo deve ser CURSO ou CERTIFICACAO");
    }
}
