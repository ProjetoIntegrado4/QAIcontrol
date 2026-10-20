package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.ColaboradoresConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/** Colaboradores com mais requisições bem-sucedidas na janela recente da base. */
@Component
@Order(70)
public class ColaboradoresSecao implements SecaoPainel {

    private final ColaboradoresConsultas repo;

    public ColaboradoresSecao(ColaboradoresConsultas repo) {
        this.repo = repo;
    }

    @Override
    public String chave() {
        return "top";
    }

    @Override
    public Set<String> requisitos() {
        return Requisitos.de(Requisitos.USO, Requisitos.FUNCIONARIOS, Requisitos.DEPARTAMENTOS);
    }

    @Override
    public Object calcular(String empresa, Disponibilidade disp) {
        return repo.maisAtivos(empresa).stream().map(t -> mapa(
                "id", t.get("id"), "cargo", t.get("cargo"), "senioridade", t.get("senioridade"),
                "area", t.get("area"), "req", inteiro(t.get("req")),
                "ferramentas", inteiro(t.get("ferramentas")), "dias", inteiro(t.get("dias")))).toList();
    }
}
