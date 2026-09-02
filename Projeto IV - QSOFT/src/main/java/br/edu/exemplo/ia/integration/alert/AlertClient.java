package br.edu.exemplo.ia.integration.alert;

import java.util.UUID;

/**
 * Porta de saída da aplicação para o serviço externo de alertas.
 * O domínio de Usage depende desta abstração, e não de HTTP/Node.js diretamente.
 */
public interface AlertClient {
    AlertResponse evaluate(UUID projectId, long tokens, String model);
}
