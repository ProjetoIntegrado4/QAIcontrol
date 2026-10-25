package br.edu.exemplo.ia.service.produtividade.importacao;

import br.edu.exemplo.ia.config.ProdutividadeProperties;
import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Carrega no PostgreSQL os CSVs da pasta configurada (produtividade.csv-dir), um arquivo por tabela:
 * {@code nome_da_tabela.csv}, com cabeçalho. Roda na subida da aplicação e só age quando a tabela
 * não existe ou está vazia; tabelas que já têm dados não são tocadas.
 *
 * <p>Não há lista fixa de tabelas: um CSV novo vira uma tabela nova. Quando a tabela não existe, os tipos
 * das colunas são deduzidos dos valores ({@link TipoColuna}), a primeira coluna vira chave primária
 * se terminar em "_id" e tiver valores únicos, e as demais colunas "_id" ganham índice.
 */
// Roda antes das demais tarefas de subida (ex.: DemonstracaoSeeder), que leem as tabelas importadas.
@Order(0)
@Component
public class ProdutividadeCsvImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdutividadeCsvImporter.class);
    private static final int LINHAS_POR_INSERT = 500;
    private static final Pattern IDENTIFICADOR = Pattern.compile("[a-z_][a-z0-9_]*");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final CatalogoBanco catalogo;
    private final Path pasta;
    private final String schema;

    public ProdutividadeCsvImporter(JdbcTemplate jdbc, TransactionTemplate tx, CatalogoBanco catalogo,
                                    ProdutividadeProperties props) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.catalogo = catalogo;
        this.pasta = Path.of(props.csvDir());
        this.schema = props.schema();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Files.isDirectory(pasta)) {
            log.info("Produtividade: pasta de CSVs {} não encontrada; usando apenas o que já está no banco.", pasta.toAbsolutePath());
            return;
        }
        List<Path> arquivos;
        try (Stream<Path> s = Files.list(pasta)) {
            arquivos = s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")).sorted().toList();
        } catch (IOException e) {
            log.error("Produtividade: não foi possível listar {}: {}", pasta, e.getMessage());
            return;
        }
        for (Path csv : arquivos) {
            String nome = csv.getFileName().toString();
            String tabela = nome.substring(0, nome.length() - 4).toLowerCase(Locale.ROOT);
            try {
                carregar(tabela, csv);
            } catch (Exception e) {
                log.error("Produtividade: falha ao importar {}: {}", csv, e.getMessage());
            }
        }
        catalogo.atualizar();
    }

    private void carregar(String tabela, Path csv) throws IOException {
        exigirIdentificador(tabela);
        String qualificada = q(schema) + "." + q(tabela);
        boolean existe = Boolean.TRUE.equals(jdbc.queryForObject("select to_regclass(?) is not null", Boolean.class,
                qualificada));
        if (existe && Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from " + qualificada + ")", Boolean.class))) {
            return;
        }
        long inicio = System.currentTimeMillis();
        List<String> colunas = cabecalho(csv);
        colunas.forEach(ProdutividadeCsvImporter::exigirIdentificador);
        List<String> tipos = existe ? tiposExistentes(tabela, colunas) : deduzir(csv, colunas.size());
        long[] total = {0};
        tx.executeWithoutResult(s -> {
            if (!existe) {
                List<String> defs = new ArrayList<>();
                for (int i = 0; i < colunas.size(); i++) defs.add(q(colunas.get(i)) + " " + tipos.get(i));
                jdbc.execute("create table " + qualificada + " (" + String.join(", ", defs) + ")");
            }
            try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
                in.readLine();
                List<List<String>> lote = new ArrayList<>();
                String linha;
                while ((linha = in.readLine()) != null) {
                    if (linha.isBlank()) continue;
                    lote.add(parse(linha));
                    if (lote.size() == LINHAS_POR_INSERT) total[0] += inserir(qualificada, colunas, tipos, lote);
                }
                total[0] += inserir(qualificada, colunas, tipos, lote);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        if (!existe) criarChaves(tabela, qualificada, colunas);
        jdbc.execute("analyze " + qualificada);
        log.info("Produtividade: {} linhas importadas em {} ({} ms).", total[0], tabela, System.currentTimeMillis() - inicio);
    }

    private static List<String> cabecalho(Path csv) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String linha = in.readLine();
            if (linha == null) throw new IllegalStateException("arquivo vazio");
            return parse(linha.replace("﻿", "")).stream().map(c -> c.trim().toLowerCase(Locale.ROOT)).toList();
        }
    }

    /** Lê o arquivo inteiro uma vez e deduz o tipo de cada coluna. */
    private List<String> deduzir(Path csv, int n) throws IOException {
        TipoColuna[] tipos = new TipoColuna[n];
        Arrays.fill(tipos, TipoColuna.INDEFINIDO);
        try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            in.readLine();
            String linha;
            while ((linha = in.readLine()) != null) {
                if (linha.isBlank()) continue;
                List<String> v = parse(linha);
                for (int i = 0; i < n && i < v.size(); i++) {
                    if (!v.get(i).isEmpty()) tipos[i] = tipos[i].combinar(TipoColuna.de(v.get(i)));
                }
            }
        }
        List<String> r = new ArrayList<>(n);
        for (TipoColuna t : tipos) r.add(t.sql());
        return r;
    }

    private List<String> tiposExistentes(String tabela, List<String> colunas) {
        List<String> r = new ArrayList<>();
        for (String c : colunas) {
            List<String> tipo = jdbc.queryForList("""
                    select data_type from information_schema.columns
                    where table_schema = ? and table_name = ? and column_name = ?""", String.class, schema, tabela, c);
            if (tipo.isEmpty()) throw new IllegalStateException("coluna " + c + " não existe na tabela " + tabela);
            r.add(tipo.get(0));
        }
        return r;
    }

    /** Chave primária na primeira coluna "_id" com valores únicos; índice nas demais colunas "_id". */
    private void criarChaves(String tabela, String qualificada, List<String> colunas) {
        String primeira = colunas.get(0);
        if (primeira.endsWith("_id") && Boolean.TRUE.equals(jdbc.queryForObject(
                "select count(*) = count(distinct " + q(primeira) + ") from " + qualificada, Boolean.class))) {
            jdbc.execute("alter table " + qualificada + " add primary key (" + q(primeira) + ")");
        }
        Set<String> feitas = new HashSet<>(Set.of(primeira));
        for (String c : colunas) {
            if (c.endsWith("_id") && feitas.add(c)) {
                jdbc.execute("create index if not exists " + q("ix_" + tabela + "_" + c) + " on " + qualificada + " (" + q(c) + ")");
            }
        }
    }

    private int inserir(String qualificada, List<String> colunas, List<String> tipos, List<List<String>> lote) {
        if (lote.isEmpty()) return 0;
        String valores = tipos.stream().map(t -> "cast(nullif(?, '') as " + t + ")")
                .collect(Collectors.joining(", ", "(", ")"));
        String sql = "insert into " + qualificada + " (" + colunas.stream().map(ProdutividadeCsvImporter::q)
                .collect(Collectors.joining(", ")) + ") values " + String.join(", ", Collections.nCopies(lote.size(), valores));
        int n = colunas.size();
        Object[] params = new Object[lote.size() * n];
        for (int i = 0; i < lote.size(); i++) {
            List<String> r = lote.get(i);
            for (int j = 0; j < n; j++) params[i * n + j] = j < r.size() ? r.get(j) : null;
        }
        int linhas = lote.size();
        jdbc.update(sql, params);
        lote.clear();
        return linhas;
    }

    private static void exigirIdentificador(String nome) {
        if (!IDENTIFICADOR.matcher(nome).matches()) throw new IllegalStateException("nome inválido para tabela/coluna: " + nome);
    }

    private static String q(String identificador) {
        return "\"" + identificador + "\"";
    }

    /** CSV simples (RFC 4180) em uma linha: vírgula como separador e aspas duplas para campos com vírgula. */
    static List<String> parse(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (aspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') { atual.append('"'); i++; }
                else if (c == '"') aspas = false;
                else atual.append(c);
            } else if (c == '"') aspas = true;
            else if (c == ',') { campos.add(atual.toString()); atual.setLength(0); }
            else atual.append(c);
        }
        campos.add(atual.toString());
        return campos;
    }
}
