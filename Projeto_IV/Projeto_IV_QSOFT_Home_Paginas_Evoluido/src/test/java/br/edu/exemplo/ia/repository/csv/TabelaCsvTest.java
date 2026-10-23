package br.edu.exemplo.ia.repository.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabelaCsvTest {

    @TempDir
    Path pasta;

    private static Map<String, String> linha(String... chaveValor) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < chaveValor.length; i += 2) {
            m.put(chaveValor[i], chaveValor[i + 1]);
        }
        return m;
    }

    @Test
    void deveLerCabecalhoELinhasRespeitandoAspas() throws IOException {
        Path arquivo = pasta.resolve("a.csv");
        Files.writeString(arquivo, "id,nome,areas\n1,Ana,\"RH, Vendas\"\n2,\"Bruno \"\"B\"\"\",\n\n");
        TabelaCsv tabela = new TabelaCsv(arquivo);

        assertEquals(List.of("id", "nome", "areas"), tabela.cabecalho());
        assertEquals(2, tabela.linhas().size());
        assertEquals("RH, Vendas", tabela.linhas().get(0).get("areas"));
        assertEquals("Bruno \"B\"", tabela.linhas().get(1).get("nome"));
        assertEquals("", tabela.linhas().get(1).get("areas"));
    }

    @Test
    void deveAcrescentarNoFimSemAlterarAsLinhasExistentes() throws IOException {
        Path arquivo = pasta.resolve("a.csv");
        Files.writeString(arquivo, "id,nome\n1,Ana");
        TabelaCsv tabela = new TabelaCsv(arquivo);

        tabela.adicionar(linha("id", "2", "nome", "Silva, Bruno"));

        assertEquals("id,nome\n1,Ana\n2,\"Silva, Bruno\"\n", Files.readString(arquivo));
        assertEquals("Silva, Bruno", tabela.linhas().get(1).get("nome"));
    }

    @Test
    void deveAcrescentarColunasNovasAoCabecalho() throws IOException {
        Path arquivo = pasta.resolve("a.csv");
        Files.writeString(arquivo, "id,nome\r\n1,Ana\r\n");
        TabelaCsv tabela = new TabelaCsv(arquivo);

        tabela.adicionar(linha("id", "2", "nome", "Bia", "cpf", "123"));

        assertEquals("id,nome,cpf\r\n1,Ana,\r\n2,Bia,123\r\n", Files.readString(arquivo));
        assertFalse(Files.exists(pasta.resolve("a.csv.gravando")));
    }

    @Test
    void deveCriarArquivoComCabecalhoQuandoNaoExiste() throws IOException {
        Path arquivo = pasta.resolve("sub").resolve("novo.csv");
        TabelaCsv tabela = new TabelaCsv(arquivo);
        assertEquals(List.of(), tabela.linhas());
        assertEquals("ausente", tabela.versao());

        tabela.adicionar(linha("id", "1", "nome", "Ana"));

        assertEquals("id,nome\n1,Ana\n", Files.readString(arquivo));
    }

    @Test
    void deveReconhecerLinhasAdicionadasPorFora() throws IOException {
        Path arquivo = pasta.resolve("a.csv");
        Files.writeString(arquivo, "id,nome\n1,Ana\n");
        TabelaCsv tabela = new TabelaCsv(arquivo);
        assertEquals(1, tabela.linhas().size());
        String antes = tabela.versao();

        Files.writeString(arquivo, "2,Bia\n", StandardOpenOption.APPEND);
        Files.setLastModifiedTime(arquivo, FileTime.fromMillis(System.currentTimeMillis() + 5000));

        assertNotEquals(antes, tabela.versao());
        assertEquals(2, tabela.linhas().size());
    }

    @Test
    void deveAceitarPontoEVirgulaBomEWindows1252() throws IOException {
        Path arquivo = pasta.resolve("excel.csv");
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        Files.write(arquivo, bom);
        Files.writeString(arquivo, "id;nome\n1;José\n", StandardOpenOption.APPEND);
        TabelaCsv tabela = new TabelaCsv(arquivo);
        assertEquals(List.of("id", "nome"), tabela.cabecalho());
        assertEquals("José", tabela.linhas().get(0).get("nome"));

        tabela.adicionar(linha("id", "2", "nome", "Ana", "cpf", "1"));
        byte[] gravado = Files.readAllBytes(arquivo);
        assertArrayEquals(bom, java.util.Arrays.copyOf(gravado, 3));
        assertTrue(Files.readString(arquivo).endsWith("id;nome;cpf\n1;José;\n2;Ana;1\n"));

        Path latin = pasta.resolve("latin.csv");
        Files.write(latin, "id,nome\n1,João\n".getBytes(Charset.forName("windows-1252")));
        TabelaCsv tabelaLatin = new TabelaCsv(latin);
        assertEquals("João", tabelaLatin.linhas().get(0).get("nome"));
        tabelaLatin.adicionar(linha("id", "2", "nome", "Conceição"));
        assertEquals("id,nome\n1,João\n2,Conceição\n",
                new String(Files.readAllBytes(latin), Charset.forName("windows-1252")));
    }

    @Test
    void deveExplicarFalhaDeGravacao() throws IOException {
        Path pastaNoLugarDoArquivo = pasta.resolve("bloqueado.csv");
        Files.createDirectory(pastaNoLugarDoArquivo);
        TabelaCsv tabela = new TabelaCsv(pastaNoLugarDoArquivo);

        BaseDadosException ex = assertThrows(BaseDadosException.class,
                () -> tabela.adicionar(linha("id", "1")));
        assertTrue(ex.getMessage().contains("bloqueado.csv"));
        assertTrue(ex.getMessage().contains("Excel"));
    }

    @Test
    void utilitariosDeTexto() {
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), TabelaCsv.interpretar("a,b\r\nc,d", ','));
        assertEquals(List.of(List.of("x\ny", "z")), TabelaCsv.interpretar("\"x\ny\",z\n", ','));
        assertEquals(';', TabelaCsv.detectarSeparador("a;b;c"));
        assertEquals(',', TabelaCsv.detectarSeparador("a,b;c,d\n1;2"));
        assertEquals("\"a;b\"", TabelaCsv.escapar("a;b", ';'));
        assertEquals("a;b", TabelaCsv.escapar("a;b", ','));
        assertEquals("x,,z", TabelaCsv.serializar(List.of("a", "b", "c"), linha("a", "x", "c", "z"), ','));
        assertEquals(StandardCharsets.UTF_8, StandardCharsets.UTF_8);
    }
}
