package br.edu.exemplo.ia.repository.csv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um arquivo CSV da base de dados, com cabeçalho na primeira linha.
 *
 * <p>A leitura é refeita sempre que o arquivo muda no disco (data de modificação ou tamanho), então
 * linhas acrescentadas por fora do sistema aparecem na próxima consulta. A gravação acrescenta uma linha
 * no fim do arquivo sem mexer nas demais; se a linha tiver colunas que o arquivo ainda não tem, elas são
 * acrescentadas ao cabeçalho e ficam vazias nas linhas antigas.
 *
 * <p>Aceita vírgula ou ponto e vírgula como separador (o Excel em português salva com ponto e vírgula),
 * UTF-8 (com ou sem BOM) ou Windows-1252, e quebras de linha LF ou CRLF. A gravação mantém o formato que
 * o arquivo já tinha.
 */
public class TabelaCsv {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final Path arquivo;
    private Conteudo cache;

    public TabelaCsv(Path arquivo) {
        this.arquivo = arquivo;
    }

    public Path arquivo() {
        return arquivo;
    }

    /** Muda sempre que o arquivo é alterado no disco. */
    public synchronized String versao() {
        try {
            if (!Files.exists(arquivo)) {
                return "ausente";
            }
            BasicFileAttributes atributos = Files.readAttributes(arquivo, BasicFileAttributes.class);
            return atributos.lastModifiedTime().toMillis() + ":" + atributos.size();
        } catch (IOException e) {
            throw falha("ler", e);
        }
    }

    /** Linhas do arquivo como mapas coluna -> valor (valores ausentes vêm como texto vazio). */
    public synchronized List<Map<String, String>> linhas() {
        return carregar().linhas;
    }

    public synchronized List<String> cabecalho() {
        return carregar().cabecalho;
    }

    /**
     * Acrescenta uma linha no fim do arquivo. Cria o arquivo, com cabeçalho, se ele não existir.
     * Colunas que o arquivo ainda não tem são acrescentadas ao cabeçalho.
     */
    public synchronized void adicionar(Map<String, String> linha) {
        Conteudo atual = carregar();
        try {
            if (!Files.exists(arquivo)) {
                Files.createDirectories(arquivo.toAbsolutePath().getParent());
                List<String> cabecalho = new ArrayList<>(linha.keySet());
                gravarTudo(new Conteudo("", cabecalho, List.of(), ',', "\n", StandardCharsets.UTF_8, false), linha);
            } else if (atual.cabecalho.containsAll(linha.keySet())) {
                acrescentarNoFim(atual, linha);
            } else {
                List<String> cabecalho = new ArrayList<>(atual.cabecalho);
                linha.keySet().stream().filter(c -> !cabecalho.contains(c)).forEach(cabecalho::add);
                gravarTudo(atual.comCabecalho(cabecalho), linha);
            }
        } catch (IOException e) {
            throw falha("gravar", e);
        } finally {
            cache = null;
        }
    }

    private Conteudo carregar() {
        String versao = versao();
        if (cache != null && cache.versao.equals(versao)) {
            return cache;
        }
        if ("ausente".equals(versao)) {
            cache = new Conteudo(versao, List.of(), List.of(), ',', "\n", StandardCharsets.UTF_8, false);
            return cache;
        }
        try {
            byte[] bytes = Files.readAllBytes(arquivo);
            boolean bom = comecaComBom(bytes);
            int inicio = bom ? BOM.length : 0;
            Charset charset = StandardCharsets.UTF_8;
            String texto;
            try {
                texto = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes, inicio, bytes.length - inicio)).toString();
            } catch (CharacterCodingException e) {
                charset = WINDOWS_1252;
                texto = new String(bytes, inicio, bytes.length - inicio, WINDOWS_1252);
            }
            String quebra = texto.contains("\r\n") ? "\r\n" : "\n";
            char separador = detectarSeparador(texto);
            List<List<String>> registros = interpretar(texto, separador);
            List<String> cabecalho = registros.isEmpty() ? List.of()
                    : registros.get(0).stream().map(String::trim).toList();
            List<Map<String, String>> linhas = new ArrayList<>();
            for (List<String> registro : registros.subList(Math.min(1, registros.size()), registros.size())) {
                Map<String, String> linha = new LinkedHashMap<>();
                for (int i = 0; i < cabecalho.size(); i++) {
                    linha.put(cabecalho.get(i), i < registro.size() ? registro.get(i).trim() : "");
                }
                linhas.add(Collections.unmodifiableMap(linha));
            }
            cache = new Conteudo(versao, cabecalho, List.copyOf(linhas), separador, quebra, charset, bom);
            return cache;
        } catch (IOException e) {
            throw falha("ler", e);
        }
    }

    private void acrescentarNoFim(Conteudo atual, Map<String, String> linha) throws IOException {
        StringBuilder texto = new StringBuilder();
        if (!terminaComQuebra()) {
            texto.append(atual.quebra);
        }
        texto.append(serializar(atual.cabecalho, linha, atual.separador)).append(atual.quebra);
        Files.write(arquivo, texto.toString().getBytes(atual.charset), StandardOpenOption.APPEND);
    }

    private void gravarTudo(Conteudo conteudo, Map<String, String> novaLinha) throws IOException {
        StringBuilder texto = new StringBuilder();
        texto.append(serializar(conteudo.cabecalho, null, conteudo.separador)).append(conteudo.quebra);
        for (Map<String, String> linha : conteudo.linhas) {
            texto.append(serializar(conteudo.cabecalho, linha, conteudo.separador)).append(conteudo.quebra);
        }
        texto.append(serializar(conteudo.cabecalho, novaLinha, conteudo.separador)).append(conteudo.quebra);
        byte[] corpo = texto.toString().getBytes(conteudo.charset);
        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".gravando");
        try (var saida = Files.newOutputStream(temporario)) {
            if (conteudo.bom) {
                saida.write(BOM);
            }
            saida.write(corpo);
        }
        try {
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Serializa uma linha na ordem do cabeçalho; com linha nula, serializa o próprio cabeçalho. */
    static String serializar(List<String> cabecalho, Map<String, String> linha, char separador) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cabecalho.size(); i++) {
            if (i > 0) {
                sb.append(separador);
            }
            String valor = linha == null ? cabecalho.get(i) : linha.getOrDefault(cabecalho.get(i), "");
            sb.append(escapar(valor == null ? "" : valor, separador));
        }
        return sb.toString();
    }

    static String escapar(String valor, char separador) {
        if (valor.indexOf(separador) >= 0 || valor.indexOf('"') >= 0 || valor.indexOf('\n') >= 0
                || valor.indexOf('\r') >= 0) {
            return '"' + valor.replace("\"", "\"\"") + '"';
        }
        return valor;
    }

    /** Separa o texto em registros e campos, respeitando aspas (inclusive quebras de linha entre aspas). */
    static List<List<String>> interpretar(String texto, char separador) {
        List<List<String>> registros = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == separador) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                fecharRegistro(registros, campos, campo);
                campos = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        fecharRegistro(registros, campos, campo);
        return registros;
    }

    private static void fecharRegistro(List<List<String>> registros, List<String> campos, StringBuilder campo) {
        campos.add(campo.toString());
        campo.setLength(0);
        boolean vazio = campos.stream().allMatch(String::isBlank);
        if (!vazio) {
            registros.add(campos);
        }
    }

    /** Usa ponto e vírgula quando a linha de cabeçalho tem mais ponto e vírgula do que vírgulas. */
    static char detectarSeparador(String texto) {
        int fim = texto.indexOf('\n');
        String cabecalho = fim < 0 ? texto : texto.substring(0, fim);
        long virgulas = cabecalho.chars().filter(c -> c == ',').count();
        long pontoVirgulas = cabecalho.chars().filter(c -> c == ';').count();
        return pontoVirgulas > virgulas ? ';' : ',';
    }

    private boolean terminaComQuebra() throws IOException {
        long tamanho = Files.size(arquivo);
        if (tamanho == 0) {
            return true;
        }
        try (var canal = Files.newByteChannel(arquivo)) {
            canal.position(tamanho - 1);
            ByteBuffer ultimo = ByteBuffer.allocate(1);
            canal.read(ultimo);
            return ultimo.get(0) == '\n';
        }
    }

    private static boolean comecaComBom(byte[] bytes) {
        return bytes.length >= 3 && bytes[0] == BOM[0] && bytes[1] == BOM[1] && bytes[2] == BOM[2];
    }

    private BaseDadosException falha(String acao, IOException e) {
        return new BaseDadosException("Não foi possível " + acao + " " + arquivo.toAbsolutePath()
                + ". Verifique se a pasta da base de dados está acessível e se o arquivo não está aberto"
                + " em outro programa (ex.: Excel). Detalhe: " + e.getMessage(), e);
    }

    private record Conteudo(String versao, List<String> cabecalho, List<Map<String, String>> linhas,
                            char separador, String quebra, Charset charset, boolean bom) {
        Conteudo comCabecalho(List<String> novoCabecalho) {
            return new Conteudo(versao, List.copyOf(novoCabecalho), linhas, separador, quebra, charset, bom);
        }
    }
}
