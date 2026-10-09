package br.edu.exemplo.ia.domain.vo;

public final class Cpf {
    private Cpf() {
    }

    /**
     * Remove pontuação e exige 11 dígitos. Retorna apenas os dígitos.
     */
    public static String normalizar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatório");
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new IllegalArgumentException(campo + " deve ter 11 dígitos");
        }
        return digitos;
    }
}
