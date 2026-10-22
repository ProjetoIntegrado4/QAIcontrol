package br.edu.exemplo.ia.service.custos;

import br.edu.exemplo.ia.dto.CustoItemResponse;
import br.edu.exemplo.ia.dto.CustoOpcoesResponse;
import br.edu.exemplo.ia.dto.CustoRequest;
import br.edu.exemplo.ia.dto.CustoResponse;
import br.edu.exemplo.ia.dto.OpcaoCustoResponse;
import br.edu.exemplo.ia.repository.custos.Agrupamento;
import br.edu.exemplo.ia.repository.custos.CustosConsultas;
import br.edu.exemplo.ia.repository.custos.FiltroCusto;
import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import br.edu.exemplo.ia.repository.produtividade.ConsultaPainel;
import br.edu.exemplo.ia.service.produtividade.secao.Disponibilidade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;

/**
 * Calcula os custos de IA (US PP-18) a partir de ai_usage_logs e dos preços de ai_models.
 * O banco soma os tokens por dimensão e modelo; o preço é aplicado pela {@link RegraCusto}.
 * Se as tabelas não existirem, a resposta vem sem total e com o motivo em "indisponiveis" (sem erro 500).
 */
@Service
public class CustosService implements CustosUseCase {

    static final String SEM_USO = "Nenhum uso de IA registrado no período selecionado";

    /** Tabelas e colunas lógicas (produtividade.yml) sem as quais não há cálculo. */
    static final Set<String> REQUISITOS_CALCULO = Set.of(
            "uso_custo.data", "uso_custo.empresa", "uso_custo.departamento", "uso_custo.funcionario",
            "uso_custo.ferramenta", "uso_custo.modelo", "uso_custo.aplicacao", "uso_custo.tokens_entrada",
            "uso_custo.tokens_saida", "uso_custo.status", "modelos.id", "modelos.preco_entrada", "modelos.preco_saida",
            "empresas.id", "departamentos.id", "departamentos.empresa", "departamentos.nome",
            "funcionarios.id", "funcionarios.empresa", "funcionarios.cargo", "funcionarios.departamento");
    static final Set<String> REQUISITOS_OPCOES = Set.of("empresas.id", "empresas.nome",
            "departamentos.id", "departamentos.empresa", "departamentos.nome",
            "funcionarios.id", "funcionarios.empresa", "funcionarios.cargo", "funcionarios.departamento");
    static final Set<String> REQUISITOS_FERRAMENTAS = Set.of("ferramentas.id", "ferramentas.nome");

    private final CustosConsultas repo;
    private final ConsultaPainel db;
    private final CatalogoBanco catalogo;
    private final RegraCusto regra;

    public CustosService(CustosConsultas repo, ConsultaPainel db, CatalogoBanco catalogo, RegraCusto regra) {
        this.repo = repo;
        this.db = db;
        this.catalogo = catalogo;
        this.regra = regra;
    }

    @Override
    public CustoOpcoesResponse opcoes(String empresaId) {
        catalogo.atualizar();
        Disponibilidade disp = new Disponibilidade(catalogo);
        if (!disp.tem("opcoes", REQUISITOS_OPCOES)) {
            return new CustoOpcoesResponse(List.of(), List.of(), List.of(), null, null, disp.faltas());
        }
        String empresa = texto(empresaId);
        return db.consultar(() -> {
            List<OpcaoCustoResponse> empresas = repo.empresas().stream()
                    .map(e -> new OpcaoCustoResponse(str(e.get("id")), str(e.get("nome")), null)).toList();
            List<OpcaoCustoResponse> departamentos = List.of();
            List<OpcaoCustoResponse> colaboradores = List.of();
            if (empresa != null) {
                if (empresas.stream().noneMatch(e -> empresa.equals(e.id()))) {
                    throw new IllegalArgumentException("Empresa não encontrada: " + empresa);
                }
                departamentos = repo.departamentos(empresa).stream()
                        .map(d -> new OpcaoCustoResponse(str(d.get("id")), str(d.get("nome")), null)).toList();
                colaboradores = repo.colaboradores(empresa).stream()
                        .map(c -> new OpcaoCustoResponse(str(c.get("id")), nomeColaborador(c), str(c.get("departamento"))))
                        .toList();
            }
            LocalDate minima = null;
            LocalDate maxima = null;
            if (disp.tem("periodo", Set.of("uso_custo.data"))) {
                Map<String, Object> p = repo.periodo();
                minima = p.get("inicio") == null ? null : LocalDate.parse(str(p.get("inicio")));
                maxima = p.get("fim") == null ? null : LocalDate.parse(str(p.get("fim")));
            }
            return new CustoOpcoesResponse(empresas, departamentos, colaboradores, minima, maxima, disp.faltas());
        });
    }

    @Override
    public CustoResponse calcular(CustoRequest request) {
        String empresa = texto(request.empresaId());
        if (empresa == null) throw new IllegalArgumentException("Empresa obrigatória");
        LocalDate inicio = data(request.inicio(), "início");
        LocalDate fim = data(request.fim(), "fim");
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Data de início deve ser anterior ou igual à data de fim");
        }
        String departamento = texto(request.departamentoId());
        String colaborador = texto(request.colaboradorId());
        if (departamento != null && colaborador != null) {
            throw new IllegalArgumentException("Informe apenas um filtro: departamento ou colaborador");
        }

        Disponibilidade disp = new Disponibilidade(catalogo);
        if (!disp.tem("custos", REQUISITOS_CALCULO)) {
            return indisponivel(disp.faltas());
        }
        boolean nomesFerramentas = disp.tem("custos.ferramentas", REQUISITOS_FERRAMENTAS);

        return db.consultar(() -> {
            if (!repo.empresaExiste(empresa)) throw new IllegalArgumentException("Empresa não encontrada: " + empresa);
            if (departamento != null && !repo.departamentoDaEmpresa(empresa, departamento)) {
                throw new IllegalArgumentException("Departamento não pertence à empresa informada");
            }
            if (colaborador != null && !repo.colaboradorDaEmpresa(empresa, colaborador)) {
                throw new IllegalArgumentException("Colaborador não pertence à empresa informada");
            }

            FiltroCusto filtro = new FiltroCusto(empresa, inicio, fim, departamento, colaborador,
                    regra.statusCobrados(), regra.aplicacoesAgente());
            Map<String, BigDecimal[]> precos = new HashMap<>();
            for (Map<String, Object> m : repo.precos()) {
                precos.put(str(m.get("id")), new BigDecimal[]{decimal(m.get("preco_entrada")), decimal(m.get("preco_saida"))});
            }

            Soma total = new Soma();
            repo.uso(filtro, Agrupamento.TOTAL).forEach(l -> total.somar(l, precos));

            Map<String, String> nomesDep = nomes(repo.departamentos(empresa), "nome");
            Map<String, String> nomesCol = new HashMap<>();
            repo.colaboradores(empresa).forEach(c -> nomesCol.put(str(c.get("id")), nomeColaborador(c)));
            Map<String, String> nomesFer = nomesFerramentas ? nomes(repo.ferramentas(), "nome") : Map.of();

            String mensagem = total.usos == 0 && total.semPreco == 0 ? SEM_USO
                    : total.semPreco > 0 ? total.semPreco + " usos sem preço cadastrado para o modelo não entraram no total"
                    : null;
            return new CustoResponse(
                    regra.arredondar(total.custo), regra.moeda(), total.usos, total.semPreco,
                    total.tokensEntrada, total.tokensSaida,
                    regra.arredondar(total.agentes), regra.arredondar(total.direto),
                    itens(repo.uso(filtro, Agrupamento.DEPARTAMENTO), precos, nomesDep, Integer.MAX_VALUE),
                    itens(repo.uso(filtro, Agrupamento.COLABORADOR), precos, nomesCol, regra.limiteColaboradores()),
                    itens(repo.uso(filtro, Agrupamento.FERRAMENTA), precos, nomesFer, Integer.MAX_VALUE),
                    regra.versao(), regra.vigencia(), regra.formula(), regra.statusCobrados(), regra.aplicacoesAgente(),
                    mensagem, disp.faltas());
        });
    }

    private CustoResponse indisponivel(Map<String, String> faltas) {
        return new CustoResponse(null, regra.moeda(), 0, 0, 0, 0, null, null, List.of(), List.of(), List.of(),
                regra.versao(), regra.vigencia(), regra.formula(), regra.statusCobrados(), regra.aplicacoesAgente(),
                "Cálculo de custos indisponível: " + faltas.get("custos"), faltas);
    }

    /** Soma as linhas por id, aplica o preço e devolve do maior para o menor custo, até o limite. */
    private List<CustoItemResponse> itens(List<Map<String, Object>> linhas, Map<String, BigDecimal[]> precos,
                                          Map<String, String> nomes, int limite) {
        Map<String, Soma> porId = new LinkedHashMap<>();
        for (Map<String, Object> l : linhas) porId.computeIfAbsent(str(l.get("id")), k -> new Soma()).somar(l, precos);
        return porId.entrySet().stream()
                .map(e -> new CustoItemResponse(e.getKey(), nomes.getOrDefault(e.getKey(), e.getKey()),
                        regra.arredondar(e.getValue().custo), e.getValue().usos, e.getValue().semPreco))
                .sorted(Comparator.comparing(CustoItemResponse::custo).reversed().thenComparing(CustoItemResponse::id))
                .limit(limite)
                .toList();
    }

    /** Acumula linhas "modelo × agente" do banco: custo dos usos com preço e contagem dos sem preço. */
    private final class Soma {
        BigDecimal custo = BigDecimal.ZERO;
        BigDecimal agentes = BigDecimal.ZERO;
        BigDecimal direto = BigDecimal.ZERO;
        long usos;
        long semPreco;
        long tokensEntrada;
        long tokensSaida;

        void somar(Map<String, Object> linha, Map<String, BigDecimal[]> precos) {
            long n = inteiro(linha.get("usos"));
            long entrada = inteiro(linha.get("tokens_entrada"));
            long saida = inteiro(linha.get("tokens_saida"));
            BigDecimal[] preco = precos.get(str(linha.get("modelo")));
            BigDecimal c = preco == null ? null : regra.custo(entrada, saida, preco[0], preco[1]);
            if (c == null) {
                semPreco += n;
                return;
            }
            custo = custo.add(c);
            if (Boolean.TRUE.equals(linha.get("agente"))) agentes = agentes.add(c);
            else direto = direto.add(c);
            usos += n;
            tokensEntrada += entrada;
            tokensSaida += saida;
        }
    }

    private static Map<String, String> nomes(List<Map<String, Object>> linhas, String coluna) {
        Map<String, String> m = new HashMap<>();
        linhas.forEach(l -> m.put(str(l.get("id")), str(l.get(coluna))));
        return m;
    }

    /** Os dados não têm nome de colaborador: o rótulo é o id com o cargo. */
    private static String nomeColaborador(Map<String, Object> c) {
        String cargo = str(c.get("cargo"));
        return cargo == null ? str(c.get("id")) : c.get("id") + " – " + cargo;
    }

    private static LocalDate data(String valor, String campo) {
        String v = texto(valor);
        if (v == null) throw new IllegalArgumentException("Data de " + campo + " obrigatória");
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data de " + campo + " inválida (use AAAA-MM-DD)");
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String str(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? null : new BigDecimal(valor.toString());
    }
}
