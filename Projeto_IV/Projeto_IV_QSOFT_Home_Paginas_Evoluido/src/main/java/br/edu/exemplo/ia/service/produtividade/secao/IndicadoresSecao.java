package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.IndicadoresConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static br.edu.exemplo.ia.service.produtividade.Valores.arredondar;
import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;
import static br.edu.exemplo.ia.service.produtividade.Valores.percentual;

/**
 * Faixa de indicadores do topo. A base (adoção e uso) é obrigatória; custo, orçamento, licenças,
 * política, treinamento e segurança entram quando as tabelas existem e ficam nulos quando não.
 */
@Component
@Order(10)
public class IndicadoresSecao implements SecaoPainel {

    private final IndicadoresConsultas repo;

    public IndicadoresSecao(IndicadoresConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "kpis";
    }

    @Override
    public Set<String> requisitos() {
        return Requisitos.de(Requisitos.USO, Requisitos.ATIVOS);
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        Map<String, Object> k = new HashMap<>(repo.uso(empresa));
        juntar(k, disp, "kpis.politica", Requisitos.de(Requisitos.USO, Requisitos.POLITICA), () -> repo.politica(empresa));
        juntar(k, disp, "kpis.custo", Requisitos.CUSTO, () -> repo.custo(empresa));
        juntar(k, disp, "kpis.orcamento", Requisitos.ORCAMENTO, () -> repo.orcamento(empresa));
        juntar(k, disp, "kpis.licencas", Requisitos.de(Requisitos.USO, Requisitos.LICENCAS), () -> repo.licencas(empresa));
        juntar(k, disp, "kpis.treinamento", Requisitos.TREINAMENTOS, () -> repo.treinamento(empresa));
        juntar(k, disp, "kpis.seguranca", Requisitos.SEGURANCA, () -> repo.seguranca(empresa));

        long func = inteiro(k.get("funcionarios")), req = inteiro(k.get("requisicoes"));
        long total = inteiro(k.get("requisicoes_total")), trein = inteiro(k.get("trein_total"));
        return mapa(
                "funcionarios", func,
                "usuarios", inteiro(k.get("usuarios")),
                "adocao", arredondar(100.0 * inteiro(k.get("usuarios_ativos")) / Math.max(func, 1), 1),
                "requisicoes", req,
                "tokens", inteiro(k.get("tokens")),
                "sucesso", percentual(req, total, 1),
                "latencia", arredondar(k.get("latencia"), 0),
                "custo", arredondar(k.get("custo"), 2),
                "orcamento", arredondar(k.get("orcamento"), 2),
                "lic_ativas", inteiroOuNulo(k, "lic_ativas"),
                "lic_ociosas", inteiroOuNulo(k, "lic_ociosas"),
                "fora_politica", k.containsKey("req_fora")
                        ? arredondar(100.0 * inteiro(k.get("req_fora")) / Math.max(req, 1), 1) : null,
                "trein_concl", percentual(inteiro(k.get("trein_concluidos")), trein, 1),
                "trein_pessoas", inteiroOuNulo(k, "trein_pessoas"),
                "eventos", inteiroOuNulo(k, "eventos"),
                "eventos_graves", inteiroOuNulo(k, "eventos_graves"));
    }

    private static void juntar(Map<String, Object> k, Disponibilidade disp, String parte, Set<String> requisitos,
                               Supplier<Map<String, Object>> consulta) {
        if (disp.tem(parte, requisitos)) k.putAll(consulta.get());
    }

    private static Long inteiroOuNulo(Map<String, Object> k, String chave) {
        return k.containsKey(chave) ? inteiro(k.get(chave)) : null;
    }
}
