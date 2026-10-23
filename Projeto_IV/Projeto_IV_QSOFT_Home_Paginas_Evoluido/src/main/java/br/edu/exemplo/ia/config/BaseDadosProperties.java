package br.edu.exemplo.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Onde fica a base de dados em CSV usada pelos cadastros de empresas, funcionários e admins.
 *
 * <p>O caminho vem da variável QSOFT_DADOS_DIR (arquivo .env na raiz do projeto ou variável de ambiente).
 * Para apontar para outra pasta, troque só esse valor.
 *
 * @param dir pasta que contém companies.csv, employees.csv, departments.csv e admins.csv
 */
@ConfigurationProperties(prefix = "qsoft.dados")
public record BaseDadosProperties(@DefaultValue("dados/csv") String dir) {
}
