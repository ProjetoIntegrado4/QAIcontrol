package br.edu.exemplo.ia.service.produtividade;

import br.edu.exemplo.ia.repository.produtividade.CatalogoBanco;
import br.edu.exemplo.ia.repository.produtividade.ConsultaPainel;
import br.edu.exemplo.ia.repository.produtividade.EsquemaBase;
import br.edu.exemplo.ia.repository.produtividade.ResumoConsultas;
import br.edu.exemplo.ia.service.produtividade.secao.Disponibilidade;
import br.edu.exemplo.ia.service.produtividade.secao.Requisitos;
import br.edu.exemplo.ia.service.produtividade.secao.SecaoPainel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/**
 * Monta o painel de produtividade a partir das seções registradas ({@link SecaoPainel}).
 * Cada seção roda em paralelo, numa conexão própria; seções cujas tabelas não existem no banco
 * vêm nulas, com o motivo em "indisponiveis".
 */
@Service
public class ProdutividadeService implements ProdutividadeUseCase {

    static final String TODAS = "ALL";

    private final List<SecaoPainel> secoes;
    private final ResumoConsultas resumo;
    private final ConsultaPainel db;
    private final CatalogoBanco catalogo;
    private final EsquemaBase esquema;
    private final ExecutorService executor;

    public ProdutividadeService(List<SecaoPainel> secoes, ResumoConsultas resumo, ConsultaPainel db,
                                CatalogoBanco catalogo, EsquemaBase esquema,
                                @Qualifier("produtividadeExecutor") ExecutorService executor) {
        this.secoes = List.copyOf(secoes);
        this.resumo = resumo;
        this.db = db;
        this.catalogo = catalogo;
        this.esquema = esquema;
        this.executor = executor;
    }

    @Override
    public Map<String, Object> resumo() {
        catalogo.atualizar();
        Disponibilidade disp = new Disponibilidade(catalogo);
        if (!disp.tem("empresas", Requisitos.EMPRESAS)) {
            throw new IllegalStateException("Cadastro de empresas indisponível: " + disp.faltas().get("empresas"));
        }
        return db.consultar(() -> {
            List<Map<String, Object>> empresas = new ArrayList<>(resumo.empresas());
            empresas.add(0, mapa("id", TODAS, "nome", "Todas as empresas", "setor", empresas.size() + " empresas"));

            List<Object> periodo = List.of();
            Map<String, Object> qualidade = new LinkedHashMap<>();
            if (disp.tem("periodo", Set.of("uso.data"))) {
                Map<String, Object> p = resumo.periodo();
                if (p.get("inicio") != null) periodo = List.of(p.get("inicio"), p.get("fim"));
                qualidade.put("linhas_uso", p.get("linhas_uso"));
            }
            if (disp.tem("qualidade.uso", Set.of("uso.centro_custo"))) {
                qualidade.put("uso_sem_cc", resumo.usoSemCentroDeCusto());
            }
            if (disp.tem("qualidade.pull_requests", Set.of("pull_requests.criado_em", "pull_requests.mergeado_em",
                    "pull_requests.revisoes"))) {
                qualidade.putAll(resumo.pullRequestsInconsistentes());
            }
            if (disp.tem("qualidade.funcionarios", Set.of("funcionarios.centro_custo"))) {
                qualidade.put("sem_cc", resumo.funcionariosSemCentroDeCusto());
            }
            return mapa(
                    "atualizado_em", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "periodo", periodo,
                    "empresas", empresas,
                    "qualidade", qualidade,
                    "janela_dias", esquema.janelaRecenteDias(),
                    "tabelas", esquema.tabelasMapeadas(),
                    "valores", esquema.todosValores(),
                    "indisponiveis", disp.faltas());
        });
    }

    @Override
    public Map<String, Object> painel(String empresa) {
        if (!TODAS.equals(empresa)
                && db.consultar(resumo::empresas).stream().noneMatch(e -> empresa.equals(String.valueOf(e.get("id"))))) {
            throw new NoSuchElementException("Empresa não encontrada: " + empresa);
        }
        Disponibilidade disp = new Disponibilidade(catalogo);
        Map<String, CompletableFuture<Object>> calculos = new LinkedHashMap<>();
        for (SecaoPainel s : secoes) {
            calculos.put(s.chave(), disp.tem(s.chave(), s.requisitos())
                    ? CompletableFuture.supplyAsync(() -> db.consultar(() -> s.calcular(empresa, disp)), executor)
                    : CompletableFuture.completedFuture(null));
        }
        Map<String, Object> painel = new LinkedHashMap<>();
        try {
            calculos.forEach((chave, f) -> painel.put(chave, f.join()));
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException r ? r : e;
        }
        painel.put("indisponiveis", disp.faltas());
        return painel;
    }
}
