package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.FerramentasConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.arredondar;
import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.juntar;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/** Uso, custo, licenças e conformidade de cada ferramenta de IA. */
@Component
@Order(40)
public class FerramentasSecao implements SecaoPainel {

    private final FerramentasConsultas repo;

    public FerramentasSecao(FerramentasConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "ferramentas";
    }

    @Override
    public Set<String> requisitos() {
        return Requisitos.USO;
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        List<Map<String, Object>> f = repo.uso(empresa);
        if (disp.tem("ferramentas.cadastro", Requisitos.FERRAMENTAS)) {
            f = juntar(f, "id", repo.cadastro(), mapa("nome", null, "categoria", null));
        }
        if (disp.tem("ferramentas.custo", Requisitos.CUSTO_FERRAMENTA)) {
            f = juntar(f, "id", repo.custo(empresa), mapa("custo", 0));
        }
        if (disp.tem("ferramentas.licencas", Requisitos.LICENCAS)) {
            f = juntar(f, "id", repo.licencas(empresa), mapa("lic_ativas", 0, "lic_ociosas", 0));
        }
        if (disp.tem("ferramentas.politica", Requisitos.POLITICA)) {
            f = juntar(f, "id", repo.foraDaPolitica(empresa), mapa("req_fora", 0));
        }
        return f.stream().map(t -> {
            long req = inteiro(t.get("req"));
            Object nome = t.get("nome") != null ? t.get("nome") : t.get("id");
            return mapa(
                    "id", t.get("id"), "nome", nome, "categoria", t.get("categoria"),
                    "req", req, "usuarios", inteiro(t.get("usuarios")),
                    "custo", arredondar(t.get("custo"), 2),
                    "lic_ativas", t.containsKey("lic_ativas") ? inteiro(t.get("lic_ativas")) : null,
                    "lic_ociosas", t.containsKey("lic_ociosas") ? inteiro(t.get("lic_ociosas")) : null,
                    "fora", t.containsKey("req_fora") && req > 0
                            ? arredondar(100.0 * inteiro(t.get("req_fora")) / req, 1) : null);
        }).toList();
    }
}
