package br.edu.exemplo.ia.domain.vo;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normaliza e valida os campos informados no cadastro, de acordo com o tipo de cada um.
 * Usado pelos services antes de criar as entidades.
 */
public final class Campos {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern UF = Pattern.compile("^[A-Z]{2}$");
    private static final Set<String> STATUS_CONTA = Set.of("ATIVO", "INATIVO");

    private Campos() {
    }

    /** Texto obrigatório, sem espaços nas pontas. */
    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatório");
        }
        return valor.trim();
    }

    /** E-mail obrigatório, em minúsculas e no formato nome@dominio.ext. */
    public static String email(String valor, String campo) {
        String email = texto(valor, campo).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException(campo + " inválido");
        }
        return email;
    }

    /** Telefone com DDD: 10 dígitos (fixo) ou 11 (celular). */
    public static Long telefone(Long valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " obrigatório");
        }
        int digitos = String.valueOf(valor).length();
        if (valor <= 0 || digitos < 10 || digitos > 11) {
            throw new IllegalArgumentException(campo + " deve ter DDD e 10 ou 11 dígitos");
        }
        return valor;
    }

    /** Apenas dígitos, com a quantidade exata (CNPJ, CEP). */
    public static String digitos(String valor, int quantidade, String campo) {
        String digitos = texto(valor, campo).replaceAll("\\D", "");
        if (digitos.length() != quantidade) {
            throw new IllegalArgumentException(campo + " deve ter " + quantidade + " dígitos");
        }
        return digitos;
    }

    /** Sigla do estado com 2 letras, em maiúsculas (ex.: SP). */
    public static String uf(String valor, String campo) {
        String uf = texto(valor, campo).toUpperCase(Locale.ROOT);
        if (!UF.matcher(uf).matches()) {
            throw new IllegalArgumentException(campo + " deve ser a sigla com 2 letras (ex.: SP)");
        }
        return uf;
    }

    /** Status da conta: ATIVO ou INATIVO. */
    public static String statusConta(String valor, String campo) {
        String status = texto(valor, campo).toUpperCase(Locale.ROOT);
        if (!STATUS_CONTA.contains(status)) {
            throw new IllegalArgumentException(campo + " deve ser ATIVO ou INATIVO");
        }
        return status;
    }

    /** Senha obrigatória, mantida exatamente como digitada. */
    public static String senha(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatória");
        }
        return valor;
    }
}
