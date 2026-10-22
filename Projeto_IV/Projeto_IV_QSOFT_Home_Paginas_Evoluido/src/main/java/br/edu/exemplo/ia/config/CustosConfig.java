package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita a configuração da regra de custos de IA (seção "custos" do application.yml). */
@Configuration
@EnableConfigurationProperties(CustosProperties.class)
public class CustosConfig {
}
