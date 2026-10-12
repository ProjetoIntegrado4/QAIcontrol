package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.repository.ProdutividadeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Service
public class ProdutividadeService implements ProdutividadeUseCase {

    private static final String TODAS = "ALL";
    private static final String[] FAIXAS = {"Uso baixo", "Uso médio", "Uso alto"};

    private final ProdutividadeRepository repo;
    private final ExecutorService consultas = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "produtividade-consulta");
        t.setDaemon(true);
        return t;
    });

    public ProdutividadeService(ProdutividadeRepository repo) {
        this.repo = repo;
    }

    @Override
    public Map<String, Object> resumo() {
        return repo.consultar(() -> {
            List<Map<String, Object>> empresas = new ArrayList<>(repo.empresas());
            empresas.add(0, mapa("id", TODAS, "nome", "Todas as empresas", "setor", empresas.size() + " empresas"));
            Map<String, Object> p = repo.periodo();
            Map<String, Object> q = new LinkedHashMap<>(repo.qualidade());
            q.put("uso_sem_cc", p.get("uso_sem_cc"));
            q.put("linhas_uso", p.get("linhas_uso"));
            return mapa(
                    "atualizado_em", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "periodo", p.get("inicio") == null ? List.of() : List.of(p.get("inicio"), p.get("fim")),
                    "empresas", empresas,
                    "qualidade", q);
        });
    }

    @Override
    public Map<String, Object> painel(String empresa) {
        if (!TODAS.equals(empresa) && repo.empresas().stream().noneMatch(e -> empresa.equals(e.get("id")))) {
            throw new NoSuchElementException("Empresa não encontrada: " + empresa);
        }
        // As seções são independentes: cada uma roda em paralelo, numa conexão própria.
        Map<String, CompletableFuture<Object>> secoes = new LinkedHashMap<>();
        secoes.put("kpis", emParalelo(() -> kpis(empresa)));
        secoes.put("mensal", emParalelo(() -> mensal(empresa)));
        secoes.put("areas", emParalelo(() -> areas(empresa)));
        secoes.put("ferramentas", emParalelo(() -> ferramentas(empresa)));
        secoes.put("top", emParalelo(() -> top(empresa)));
        secoes.put("gov", emParalelo(() -> governanca(empresa)));
        secoes.put("eng", emParalelo(() -> engenharia(empresa)));
        Map<String, Object> painel = new LinkedHashMap<>();
        try {
            for (var s : secoes.entrySet()) painel.put(s.getKey(), s.getValue().join());
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException r ? r : e;
        }
        return painel;
    }

    private CompletableFuture<Object> emParalelo(Supplier<Object> secao) {
        return CompletableFuture.supplyAsync(() -> repo.consultar(secao), consultas);
    }

    private List<Map<String, Object>> mensal(String empresa) {
        return repo.mensal(empresa).stream().map(m -> mapa(
                "mes", m.get("mes"), "req", inteiro(m.get("req")), "ativos", inteiro(m.get("ativos")),
                "sucesso", r(m.get("sucesso"), 1), "custo", r(m.get("custo"), 2),
                "orcamento", r(m.get("orcamento"), 2))).toList();
    }

    private List<Map<String, Object>> areas(String empresa) {
        return repo.areas(empresa).stream().map(a -> {
            long func = inteiro(a.get("funcionarios")), usu = inteiro(a.get("usuarios"));
            return mapa("area", a.get("area"), "funcionarios", func, "usuarios", usu,
                    "req", inteiro(a.get("req")), "req_usuario", r(a.get("req_usuario"), 0),
                    "custo", r(a.get("custo"), 2), "trein", r(a.get("trein"), 1),
                    "adocao", func > 0 ? r(100.0 * usu / func, 1) : 0);
        }).toList();
    }

    private List<Map<String, Object>> ferramentas(String empresa) {
        return repo.ferramentas(empresa).stream().map(f -> mapa(
                "id", f.get("id"), "nome", f.get("nome"), "categoria", f.get("categoria"),
                "req", inteiro(f.get("req")), "usuarios", inteiro(f.get("usuarios")),
                "custo", r(f.get("custo"), 2), "lic_ativas", inteiro(f.get("lic_ativas")),
                "lic_ociosas", inteiro(f.get("lic_ociosas")), "fora", r(f.get("fora"), 1))).toList();
    }

    private List<Map<String, Object>> top(String empresa) {
        return repo.top(empresa).stream().map(t -> mapa(
                "id", t.get("id"), "cargo", t.get("cargo"), "senioridade", t.get("senioridade"),
                "area", t.get("area"), "req", inteiro(t.get("req")),
                "ferramentas", inteiro(t.get("ferramentas")), "dias", inteiro(t.get("dias")))).toList();
    }

    private Map<String, Object> governanca(String empresa) {
        return mapa(
                "eventos_tipo", contagem(repo.eventosPorTipo(empresa)),
                "eventos_sev", contagem(repo.eventosPorSeveridade(empresa)),
                "trein_status", contagem(repo.treinamentosPorSituacao(empresa)));
    }

    private Map<String, Object> kpis(String empresa) {
        Map<String, Object> k = repo.kpis(empresa);
        long func = inteiro(k.get("funcionarios")), req = inteiro(k.get("requisicoes"));
        long total = inteiro(k.get("requisicoes_total")), trein = inteiro(k.get("trein_total"));
        return mapa(
                "funcionarios", func,
                "usuarios", inteiro(k.get("usuarios")),
                "adocao", r(100.0 * inteiro(k.get("usuarios_ativos")) / Math.max(func, 1), 1),
                "requisicoes", req,
                "tokens", inteiro(k.get("tokens")),
                "sucesso", total > 0 ? r(100.0 * req / total, 1) : null,
                "latencia", r(k.get("latencia"), 0),
                "custo", r(k.get("custo"), 2),
                "orcamento", r(k.get("orcamento"), 2),
                "lic_ativas", inteiro(k.get("lic_ativas")),
                "lic_ociosas", inteiro(k.get("lic_ociosas")),
                "fora_politica", r(100.0 * inteiro(k.get("req_fora")) / Math.max(req, 1), 1),
                "trein_concl", trein > 0 ? r(100.0 * inteiro(k.get("trein_concluidos")) / trein, 1) : null,
                "trein_pessoas", inteiro(k.get("trein_pessoas")),
                "eventos", inteiro(k.get("eventos")),
                "eventos_graves", inteiro(k.get("eventos_graves")));
    }

    private Map<String, Object> engenharia(String empresa) {
        Map<String, Object> t = repo.engTotais(empresa);
        if (inteiro(t.get("prs")) == 0) return null;
        return mapa(
                "mensal", repo.engMensal(empresa).stream().map(m -> mapa(
                        "mes", m.get("mes"), "prs", inteiro(m.get("prs")), "merged", inteiro(m.get("merged")),
                        "lead", r(m.get("lead"), 0), "build_ok", r(m.get("build_ok"), 1),
                        "deploy_falha", r(m.get("deploy_falha"), 1))).toList(),
                "coortes", repo.engCoortes(empresa).stream().map(c -> mapa(
                        "faixa", FAIXAS[(int) inteiro(c.get("faixa")) - 1], "devs", inteiro(c.get("devs")),
                        "ai_mediana", inteiro(c.get("ai_mediana")), "prs", r(c.get("prs"), 1),
                        "merged", r(c.get("merged"), 1), "lead", r(c.get("lead"), 0))).toList(),
                "corr", r(repo.engCorrelacao(empresa), 2),
                "totais", mapa(
                        "prs", inteiro(t.get("prs")), "merged", inteiro(t.get("merged")), "lead", r(t.get("lead"), 0),
                        "build_ok", r(t.get("build_ok"), 1), "deploy_falha", r(t.get("deploy_falha"), 1),
                        "devs", inteiro(t.get("devs"))));
    }

    private static Map<String, Object> contagem(List<Map<String, Object>> linhas) {
        Map<String, Object> m = new LinkedHashMap<>();
        linhas.forEach(l -> m.put(String.valueOf(l.get("chave")), inteiro(l.get("total"))));
        return m;
    }

    private static long inteiro(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static Double r(Object v, int casas) {
        if (v == null) return null;
        double d = ((Number) v).doubleValue();
        if (Double.isNaN(d)) return null;
        return BigDecimal.valueOf(d).setScale(casas, RoundingMode.HALF_EVEN).doubleValue();
    }

    private static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
