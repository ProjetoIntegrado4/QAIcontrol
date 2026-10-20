package br.edu.exemplo.ia.service.produtividade.importacao;

import br.edu.exemplo.ia.config.ProdutividadeProperties;
import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdutividadeCsvImporterTest {

    private static final String EXISTE = "select to_regclass(?) is not null";

    @TempDir
    Path pasta;

    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private CatalogoBanco catalogo;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        tx = mock(TransactionTemplate.class);
        catalogo = mock(CatalogoBanco.class);
        doAnswer(i -> {
            ((Consumer<TransactionStatus>) i.getArgument(0)).accept(mock(TransactionStatus.class));
            return null;
        }).when(tx).executeWithoutResult(any());
    }

    private void importar(Path dir) {
        new ProdutividadeCsvImporter(jdbc, tx, catalogo,
                new ProdutividadeProperties("public", dir.toString(), 90, 12, "CC-", Map.of(), Map.of()))
                .run(new DefaultApplicationArguments());
    }

    @Test
    void parseDeveTratarAspasEVirgulas() {
        assertEquals(List.of("a", "b,c", "d\"e", ""), ProdutividadeCsvImporter.parse("a,\"b,c\",\"d\"\"e\","));
    }

    @Test
    void semPastaNaoDeveAcessarOBanco() {
        importar(pasta.resolve("nao-existe"));
        verify(jdbc, never()).execute(anyString());
        verify(catalogo, never()).atualizar();
    }

    @Test
    void qualquerCsvViraTabelaComTiposDeduzidosChaveEIndices() throws Exception {
        Files.writeString(pasta.resolve("Fornecedores.csv"), """
                ﻿fornecedor_id,empresa_id,nome,ativo,valor,desde
                F1,C01,"Alpha, Inc",true,10,2025-01-01

                F2,C02,Beta,false,2.5,
                """);
        Files.writeString(pasta.resolve("leia-me.txt"), "ignorado");
        when(jdbc.queryForObject(eq(EXISTE), eq(Boolean.class), anyString())).thenReturn(false);
        when(jdbc.queryForObject(startsWith("select count(*) = count(distinct"), eq(Boolean.class))).thenReturn(true);

        importar(pasta);

        verify(jdbc).execute("create table \"public\".\"fornecedores\" (\"fornecedor_id\" text, \"empresa_id\" text, "
                + "\"nome\" text, \"ativo\" boolean, \"valor\" numeric, \"desde\" date)");
        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("insert into \"public\".\"fornecedores\""), params.capture());
        assertEquals(12, params.getValue().length);
        assertEquals("Alpha, Inc", params.getValue()[2]);
        verify(jdbc).execute("alter table \"public\".\"fornecedores\" add primary key (\"fornecedor_id\")");
        verify(jdbc).execute(startsWith("create index if not exists \"ix_fornecedores_empresa_id\""));
        verify(jdbc).execute("analyze \"public\".\"fornecedores\"");
        verify(catalogo).atualizar();
    }

    @Test
    void primeiraColunaRepetidaNaoViraChavePrimaria() throws Exception {
        Files.writeString(pasta.resolve("uso.csv"), "funcionario_id,total\nE1,1\nE1,2\n");
        when(jdbc.queryForObject(eq(EXISTE), eq(Boolean.class), anyString())).thenReturn(false);
        when(jdbc.queryForObject(startsWith("select count(*) = count(distinct"), eq(Boolean.class))).thenReturn(false);

        importar(pasta);

        verify(jdbc, never()).execute(startsWith("alter table"));
        verify(jdbc, never()).execute(startsWith("create index"));
    }

    @Test
    void tabelaComDadosNaoDeveSerTocada() throws Exception {
        Files.writeString(pasta.resolve("companies.csv"), "company_id,company_name\nC01,Alpha\n");
        when(jdbc.queryForObject(eq(EXISTE), eq(Boolean.class), anyString())).thenReturn(true);
        when(jdbc.queryForObject(startsWith("select exists"), eq(Boolean.class))).thenReturn(true);

        importar(pasta);

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(jdbc, never()).execute(startsWith("create"));
    }

    @Test
    void tabelaVaziaExistenteDeveSerPreenchidaComOsTiposDoBanco() throws Exception {
        StringBuilder csv = new StringBuilder("department_id,company_id\n");
        for (int i = 0; i < 501; i++) csv.append("D").append(i).append(",C01\n");
        Files.writeString(pasta.resolve("departments.csv"), csv);
        when(jdbc.queryForObject(eq(EXISTE), eq(Boolean.class), anyString())).thenReturn(true);
        when(jdbc.queryForObject(startsWith("select exists"), eq(Boolean.class))).thenReturn(false);
        when(jdbc.queryForList(anyString(), eq(String.class), any(Object[].class))).thenReturn(List.of("character varying"));

        importar(pasta);

        verify(jdbc, times(2)).update(startsWith("insert into \"public\".\"departments\""), any(Object[].class));
        verify(jdbc, never()).execute(startsWith("create"));
    }

    @Test
    void erroNumArquivoNaoDerrubaOsOutros() throws Exception {
        Files.writeString(pasta.resolve("a.csv"), "id,nome invalido\n1,x\n");
        Files.writeString(pasta.resolve("b.csv"), "");
        Files.writeString(pasta.resolve("c.csv"), "c_id\n1\n");
        when(jdbc.queryForObject(eq(EXISTE), eq(Boolean.class), anyString())).thenReturn(false);

        importar(pasta);

        verify(jdbc, times(1)).update(anyString(), any(Object[].class));
        verify(catalogo).atualizar();
    }
}
