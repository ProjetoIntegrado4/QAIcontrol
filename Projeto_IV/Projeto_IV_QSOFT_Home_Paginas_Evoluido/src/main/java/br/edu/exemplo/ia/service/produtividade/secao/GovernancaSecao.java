package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.GovernancaConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.contagem;

/** Eventos de segurança por tipo e severidade, e treinamentos por situação. Cada contagem é opcional. */
@Component
@Order(60)
public class GovernancaSecao implements SecaoPainel {

    private final GovernancaConsultas repo;

    public GovernancaSecao(GovernancaConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "gov";
    }

    @Override
    public Set<String> requisitos() {
        return Set.of();
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        Map<String, Object> gov = new LinkedHashMap<>();
        boolean seguranca = disp.tem("gov.seguranca", Requisitos.SEGURANCA);
        gov.put("eventos_tipo", seguranca ? contagem(repo.eventosPorTipo(empresa)) : null);
        gov.put("eventos_sev", seguranca ? contagem(repo.eventosPorSeveridade(empresa)) : null);
        gov.put("trein_status", disp.tem("gov.treinamento", Requisitos.TREINAMENTOS)
                ? contagem(repo.treinamentosPorSituacao(empresa)) : null);
        return gov;
    }
}
