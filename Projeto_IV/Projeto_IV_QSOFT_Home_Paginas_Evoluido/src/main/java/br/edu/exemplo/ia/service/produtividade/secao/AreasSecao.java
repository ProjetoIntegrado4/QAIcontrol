package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.AreasConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.arredondar;
import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.juntar;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/** Adoção, volume, custo e treinamento por área (departamento). */
@Component
@Order(30)
public class AreasSecao implements SecaoPainel {

    private final AreasConsultas repo;

    public AreasSecao(AreasConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "areas";
    }

    @Override
    public Set<String> requisitos() {
        return Requisitos.de(Requisitos.USO, Requisitos.ATIVOS, Requisitos.DEPARTAMENTOS);
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        List<Map<String, Object>> areas = repo.uso(empresa);
        if (disp.tem("areas.custo", Requisitos.CUSTO_AREA)) {
            areas = juntar(areas, "area", repo.custo(empresa), mapa("custo", 0));
        }
        if (disp.tem("areas.treinamento", Requisitos.de(Requisitos.TREINAMENTOS, Requisitos.ATIVOS))) {
            areas = juntar(areas, "area", repo.treinamento(empresa), mapa("trein", null));
        }
        return areas.stream().map(a -> {
            long func = inteiro(a.get("funcionarios")), usu = inteiro(a.get("usuarios"));
            return mapa("area", a.get("area"), "funcionarios", func, "usuarios", usu,
                    "req", inteiro(a.get("req")), "req_usuario", arredondar(a.get("req_usuario"), 0),
                    "custo", arredondar(a.get("custo"), 2), "trein", arredondar(a.get("trein"), 1),
                    "adocao", func > 0 ? arredondar(100.0 * usu / func, 1) : 0);
        }).toList();
    }
}
