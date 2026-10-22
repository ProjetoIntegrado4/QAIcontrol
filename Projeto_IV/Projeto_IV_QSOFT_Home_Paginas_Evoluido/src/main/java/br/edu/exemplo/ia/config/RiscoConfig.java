package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita a configuração das recomendações por faixa de risco (seção "risco" do application.yml). */
@Configuration
@EnableConfigurationProperties(RiscoProperties.class)
public class RiscoConfig {
}
