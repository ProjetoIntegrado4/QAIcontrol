package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Ativa a configuração da base de dados em CSV dos cadastros (qsoft.dados.*). */
@Configuration
@EnableConfigurationProperties(BaseDadosProperties.class)
public class BaseDadosConfig {
}
