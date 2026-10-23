package br.edu.exemplo.ia.domain.vo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Liga o código de um registro na base de dados em CSV (ex.: C01, C01-E0001) ao UUID usado pela API.
 * O UUID é derivado do código, então o mesmo registro tem sempre o mesmo id no site.
 */
public final class CodigoCsv {
    private CodigoCsv() {
    }

    public static UUID uuid(String tipo, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do registro obrigatório");
        }
        return UUID.nameUUIDFromBytes((tipo + ":" + codigo.trim()).getBytes(StandardCharsets.UTF_8));
    }
}
