package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Utilitários de teste: carrega o produtividade.yml real, como a aplicação faz. */
public final class ProdutividadeTestes {

    private ProdutividadeTestes() {
    }

    public static ProdutividadeProperties propriedades() {
        try {
            StandardEnvironment env = new StandardEnvironment();
            new YamlPropertySourceLoader().load("produtividade", new ClassPathResource("produtividade.yml"))
                    .forEach(env.getPropertySources()::addFirst);
            return Binder.get(env).bind("produtividade", ProdutividadeProperties.class).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
