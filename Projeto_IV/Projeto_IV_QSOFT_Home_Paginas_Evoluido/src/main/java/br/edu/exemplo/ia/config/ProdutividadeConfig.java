package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Beans do painel de produtividade. */
@Configuration
@EnableConfigurationProperties(ProdutividadeProperties.class)
public class ProdutividadeConfig {

    /** Executor das seções do painel: cada seção roda em paralelo, numa conexão própria. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService produtividadeExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "produtividade-consulta");
            t.setDaemon(true);
            return t;
        });
    }
}
