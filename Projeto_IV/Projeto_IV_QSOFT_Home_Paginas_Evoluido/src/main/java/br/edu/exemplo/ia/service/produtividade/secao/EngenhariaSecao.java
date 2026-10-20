package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.EngenhariaConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.arredondar;
import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.juntar;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/**
 * Entrega de software (pull requests, builds e deploys) e a relação com o uso de IA.
 * Vem nula para empresas sem pull requests; builds e deploys são opcionais.
 */
@Component
@Order(50)
public class EngenhariaSecao implements SecaoPainel {

    private static final String[] FAIXAS = {"Uso baixo", "Uso médio", "Uso alto"};

    private final EngenhariaConsultas repo;

    public EngenhariaSecao(EngenhariaConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "eng";
    }

    @Override
    public Set<String> requisitos() {
        return Requisitos.de(Requisitos.USO, Requisitos.PULL_REQUESTS);
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        Map<String, Object> t = new HashMap<>(repo.totaisPullRequests(empresa));
        if (inteiro(t.get("prs")) == 0) return null;
        List<Map<String, Object>> mensal = repo.pullRequestsPorMes(empresa);
        if (disp.tem("eng.builds", Requisitos.BUILDS)) {
            t.putAll(repo.totaisBuilds(empresa));
            mensal = juntar(mensal, "mes", repo.buildsPorMes(empresa), mapa("build_ok", null));
        }
        if (disp.tem("eng.deploys", Requisitos.DEPLOYS)) {
            t.putAll(repo.totaisDeploys(empresa));
            mensal = juntar(mensal, "mes", repo.deploysPorMes(empresa), mapa("deploy_falha", null));
        }
        return mapa(
                "mensal", mensal.stream().map(m -> mapa(
                        "mes", m.get("mes"), "prs", inteiro(m.get("prs")), "merged", inteiro(m.get("merged")),
                        "lead", arredondar(m.get("lead"), 0), "build_ok", arredondar(m.get("build_ok"), 1),
                        "deploy_falha", arredondar(m.get("deploy_falha"), 1))).toList(),
                "coortes", repo.coortes(empresa).stream().map(c -> mapa(
                        "faixa", FAIXAS[(int) inteiro(c.get("faixa")) - 1], "devs", inteiro(c.get("devs")),
                        "ai_mediana", inteiro(c.get("ai_mediana")), "prs", arredondar(c.get("prs"), 1),
                        "merged", arredondar(c.get("merged"), 1), "lead", arredondar(c.get("lead"), 0))).toList(),
                "corr", arredondar(repo.correlacao(empresa), 2),
                "totais", mapa(
                        "prs", inteiro(t.get("prs")), "merged", inteiro(t.get("merged")), "lead", arredondar(t.get("lead"), 0),
                        "build_ok", arredondar(t.get("build_ok"), 1), "deploy_falha", arredondar(t.get("deploy_falha"), 1),
                        "devs", inteiro(t.get("devs"))),
                "empresas_com_projetos", repo.empresasComProjetos());
    }
}
