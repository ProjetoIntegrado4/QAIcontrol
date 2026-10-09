package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record AdminVO(
        String nome,
        String cpf,
        String email,
        Long telefone,
        String cargo,
        String senhaHash,
        String statusConta,
        String nivelAcesso
) {
    public AdminVO {
        requireText(nome, "Nome do admin");
        requireText(email, "E-mail do admin");
        requirePositive(telefone, "Telefone do admin");
        requireText(cargo, "Cargo do admin");
        requireText(senhaHash, "Senha do admin");
        requireText(statusConta, "Status da conta do admin");
        requireText(nivelAcesso, "Nível de acesso do admin");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " obrigatório");
        }
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " deve ser um número inteiro positivo");
        }
    }

    @Override
    public String toString() {
        return "AdminVO[nome=" + nome + ", cpf=" + cpf + ", email=" + email + ", telefone=" + telefone
                + ", cargo=" + cargo + ", senhaHash=[REDACTED], statusConta=" + statusConta
                + ", nivelAcesso=" + nivelAcesso + "]";
    }
}