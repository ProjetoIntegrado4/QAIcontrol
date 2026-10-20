package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.MensalConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.arredondar;
import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.juntar;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/** Evolução mensal de uso, custo faturado e orçamento. */
@Component
@Order(20)
public class MensalSecao implements SecaoPainel {

    private final MensalConsultas repo;

    public MensalSecao(MensalConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "mensal";
    }

    @Override
    public Set<String> requisitos() {
        return Requisitos.USO;
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        List<Map<String, Object>> meses = repo.uso(empresa);
        if (disp.tem("mensal.custo", Requisitos.CUSTO_MENSAL)) {
            meses = juntar(meses, "mes", repo.custo(empresa), mapa("custo", 0));
        }
        if (disp.tem("mensal.orcamento", Requisitos.ORCAMENTO_MENSAL)) {
            meses = juntar(meses, "mes", repo.orcamento(empresa), mapa("orcamento", 0));
        }
        return meses.stream().map(m -> mapa(
                "mes", m.get("mes"), "req", inteiro(m.get("req")), "ativos", inteiro(m.get("ativos")),
                "sucesso", arredondar(m.get("sucesso"), 1), "custo", arredondar(m.get("custo"), 2),
                "orcamento", arredondar(m.get("orcamento"), 2))).toList();
    }
}
