package br.edu.exemplo.ia.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record FuncionarioVO(
        String nome,
        Long cpf,
        String emailCorporativo,
        Long telefone,
        String cargo,
        String setor,
        String matricula,
        String senha,
        String statusConta
) {
    public FuncionarioVO {
        requireText(nome, "Nome do funcionário");
        requireText(cargo, "Cargo do funcionário");
        validateOptional(cpf, "CPF do funcionário");
        validateOptional(emailCorporativo, "E-mail corporativo do funcionário");
        validateOptional(telefone, "Telefone do funcionário");
        validateOptional(setor, "Setor do funcionário");
        validateOptional(matricula, "Matrícula do funcionário");
        validateOptional(senha, "Senha do funcionário");
        validateOptional(statusConta, "Status da conta do funcionário");
    }

    public FuncionarioVO(String nome) {
        this(nome, null, null, null, "Não informado", null, null, null, "ATIVO");
    }

    public static FuncionarioVO completo(String nome, Long cpf, String emailCorporativo, Long telefone,
                                         String cargo, String setor, String matricula, String senha,
                                         String statusConta) {
        requirePositive(cpf, "CPF do funcionário");
        requireText(emailCorporativo, "E-mail corporativo do funcionário");
        requirePositive(telefone, "Telefone do funcionário");
        requireText(setor, "Setor do funcionário");
        requireText(matricula, "Matrícula do funcionário");
        requireText(senha, "Senha do funcionário");
        requireText(statusConta, "Status da conta do funcionário");
        return new FuncionarioVO(nome, cpf, emailCorporativo, telefone, cargo, setor, matricula, senha, statusConta);
    }

    private static void validateOptional(String value, String field) {
        if (value != null) {
            requireText(value, field);
        }
    }

    private static void validateOptional(Long value, String field) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(field + " deve ser um número inteiro positivo");
        }
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
        return "FuncionarioVO[nome=" + nome + ", cpf=" + cpf + ", emailCorporativo=" + emailCorporativo
                + ", telefone=" + telefone + ", cargo=" + cargo + ", setor=" + setor + ", matricula="
                + matricula + ", senha=[REDACTED], statusConta=" + statusConta + "]";
    }
}
