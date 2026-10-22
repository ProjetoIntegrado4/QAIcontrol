package br.edu.exemplo.ia.dto;

import java.util.UUID;

/**
 * Dados enviados pelo administrador na tela de Governança para publicar uma política (ou a próxima versão dela).
 * As validações ficam na entidade PoliticaUso e no PoliticaService, como em Certificacao.
 * Na nova versão a empresa pode ser omitida: ela é sempre a da política original.
 */
public record PoliticaRequest(
        UUID empresaId,
        String titulo,
        String conteudo,
        boolean obrigatoria
) {
}
