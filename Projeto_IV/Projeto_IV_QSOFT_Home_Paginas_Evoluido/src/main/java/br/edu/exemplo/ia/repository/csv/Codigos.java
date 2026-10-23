package br.edu.exemplo.ia.repository.csv;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Geração dos próximos códigos e conversões de valores usadas pelos repositórios em CSV. */
final class Codigos {
    private Codigos() {
    }

    /**
     * Próximo código no formato prefixo + número com zeros à esquerda (ex.: C19, C01-E0358).
     * Considera só os códigos existentes com o mesmo prefixo.
     */
    static String proximo(Collection<String> existentes, String prefixo, int digitos) {
        Pattern formato = Pattern.compile(Pattern.quote(prefixo) + "(\\d+)");
        int maior = 0;
        for (String codigo : existentes) {
            Matcher m = formato.matcher(codigo == null ? "" : codigo.trim());
            if (m.matches()) {
                maior = Math.max(maior, Integer.parseInt(m.group(1)));
            }
        }
        return prefixo + String.format("%0" + digitos + "d", maior + 1);
    }

    static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static Long numeroOuNulo(String valor) {
        String texto = textoOuNulo(valor);
        if (texto == null) {
            return null;
        }
        try {
            return (long) Double.parseDouble(texto.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    /** Compara nomes ignorando maiúsculas, acentos e espaços extras. */
    static String chave(String nome) {
        String semAcento = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
