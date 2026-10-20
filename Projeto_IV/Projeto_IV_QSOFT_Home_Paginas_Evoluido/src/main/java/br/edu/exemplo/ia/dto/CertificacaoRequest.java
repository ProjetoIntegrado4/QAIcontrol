package br.edu.exemplo.ia.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados enviados pela tela de Capacitações.
 * As validações (campos obrigatórios, data não futura, mesma empresa) ficam na entidade Certificacao
 * e no CertificacaoService, para a API devolver mensagens claras, como "Data de conclusão obrigatória".
 */
public record CertificacaoRequest(
        UUID funcionarioId,
        UUID empresaId,
        String nome,
        String instituicao,
        LocalDate dataConclusao,
        String tipo
) {
}
