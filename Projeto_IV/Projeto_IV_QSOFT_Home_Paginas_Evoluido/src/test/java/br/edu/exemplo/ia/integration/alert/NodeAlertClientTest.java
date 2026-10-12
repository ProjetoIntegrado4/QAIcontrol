package br.edu.exemplo.ia.integration.alert;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobe um servidor HTTP local no lugar do alert-service (Node) para testar a integração. */
class NodeAlertClientTest {

    private HttpServer server;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> resposta = new AtomicReference<>();

    @BeforeEach
    void subirServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/alerts/evaluate", exchange -> {
            corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = resposta.get();
            if (body == null) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void pararServidor() {
        server.stop(0);
    }

    private NodeAlertClient client() {
        return new NodeAlertClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void deveEnviarConsumoERetornarAlertaDoServico() {
        resposta.set("{\"level\":\"WARN\",\"message\":\"Consumo alto\"}");
        UUID empresaId = UUID.randomUUID();

        AlertResponse alerta = client().evaluate(empresaId, 15000, "gpt-demo");

        assertEquals(new AlertResponse("WARN", "Consumo alto"), alerta);
        assertTrue(corpoRecebido.get().contains(empresaId.toString()));
        assertTrue(corpoRecebido.get().contains("\"tokens\":15000"));
        assertTrue(corpoRecebido.get().contains("\"model\":\"gpt-demo\""));
    }

    @Test
    void respostaVaziaDeveVirarAlertaDesconhecido() {
        AlertResponse alerta = client().evaluate(UUID.randomUUID(), 10, "gpt-demo");

        assertEquals("UNKNOWN", alerta.level());
    }
}
