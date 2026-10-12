package br.edu.exemplo.ia.service;

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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdutividadeCsvImporterTest {

    @TempDir
    Path pasta;

    private JdbcTemplate jdbc;
    private TransactionTemplate tx;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        tx = mock(TransactionTemplate.class);
        doAnswer(i -> {
            ((Consumer<TransactionStatus>) i.getArgument(0)).accept(mock(TransactionStatus.class));
            return null;
        }).when(tx).executeWithoutResult(any());
    }

    @Test
    void parseDeveTratarAspasEVirgulas() {
        assertEquals(List.of("a", "b,c", "d\"e", ""), ProdutividadeCsvImporter.parse("a,\"b,c\",\"d\"\"e\","));
    }

    @Test
    void semPastaNaoDeveAcessarOBanco() {
        new ProdutividadeCsvImporter(jdbc, tx, pasta.resolve("nao-existe").toString())
                .run(new DefaultApplicationArguments());

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void deveCriarTabelaInserirLinhasECriarIndices() throws Exception {
        Files.writeString(pasta.resolve("companies.csv"), """
                ﻿company_id,company_name,industry,company_size,employee_count
                C01,"Alpha, Inc",Varejo,Grande,100

                C02,Beta,Saúde,Pequena,
                """);
        when(jdbc.queryForObject(eq("select to_regclass(?) is not null"), eq(Boolean.class), anyString()))
                .thenReturn(false);

        new ProdutividadeCsvImporter(jdbc, tx, pasta.toString()).run(new DefaultApplicationArguments());

        verify(jdbc).execute(startsWith("create table public.companies"));
        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("insert into public.companies"), params.capture());
        assertEquals(10, params.getValue().length);
        assertEquals("Alpha, Inc", params.getValue()[1]);
        verify(jdbc).execute("analyze public.companies");
        verify(jdbc, times(5)).execute(startsWith("create index"));
    }

    @Test
    void tabelaComDadosNaoDeveSerTocada() throws Exception {
        Files.writeString(pasta.resolve("companies.csv"), "company_id,company_name,industry,company_size,employee_count\n");
        when(jdbc.queryForObject(eq("select to_regclass(?) is not null"), eq(Boolean.class), anyString()))
                .thenReturn(true);
        when(jdbc.queryForObject(startsWith("select exists"), eq(Boolean.class))).thenReturn(true);

        new ProdutividadeCsvImporter(jdbc, tx, pasta.toString()).run(new DefaultApplicationArguments());

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(jdbc, never()).execute(startsWith("create"));
    }

    @Test
    void tabelaVaziaExistenteDeveSerPreenchidaSemRecriar() throws Exception {
        StringBuilder csv = new StringBuilder("department_id,company_id,department_name\n");
        for (int i = 0; i < 501; i++) csv.append("D").append(i).append(",C01,Area ").append(i).append('\n');
        Files.writeString(pasta.resolve("departments.csv"), csv);
        when(jdbc.queryForObject(eq("select to_regclass(?) is not null"), eq(Boolean.class), anyString()))
                .thenReturn(true);
        when(jdbc.queryForObject(startsWith("select exists"), eq(Boolean.class))).thenReturn(false);

        new ProdutividadeCsvImporter(jdbc, tx, pasta.toString()).run(new DefaultApplicationArguments());

        verify(jdbc, times(2)).update(startsWith("insert into public.departments"), any(Object[].class));
        verify(jdbc, never()).execute(startsWith("create"));
    }

    @Test
    void cabecalhoDiferenteDeveSerRegistradoSemDerrubarAImportacao() throws Exception {
        Files.writeString(pasta.resolve("companies.csv"), "id,nome\nC01,Alpha\n");
        when(jdbc.queryForObject(eq("select to_regclass(?) is not null"), eq(Boolean.class), anyString()))
                .thenReturn(false);

        new ProdutividadeCsvImporter(jdbc, tx, pasta.toString()).run(new DefaultApplicationArguments());

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(jdbc, atLeastOnce()).queryForObject(anyString(), eq(Boolean.class), anyString());
    }
}
