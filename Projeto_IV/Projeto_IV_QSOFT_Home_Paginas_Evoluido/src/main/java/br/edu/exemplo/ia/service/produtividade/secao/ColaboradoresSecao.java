package br.edu.exemplo.ia.service.produtividade.secao;

import br.edu.exemplo.ia.repository.produtividade.ColaboradoresConsultas;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import java.util.HashMap;
import java.util.Map;

import java.util.Set;

import static br.edu.exemplo.ia.service.produtividade.Valores.inteiro;
import static br.edu.exemplo.ia.service.produtividade.Valores.mapa;

/** Colaboradores com mais requisições bem-sucedidas na janela recente da base. */
@Component
@Order(70)
public class ColaboradoresSecao implements SecaoPainel {

    private final ColaboradoresConsultas repo;
    private final FuncionarioRepository funcionarios;

    public ColaboradoresSecao(ColaboradoresConsultas repo) {
        this(repo, null);
    }

    /** Com o cadastro de funcionários, o ranking mostra o nome de cada colaborador ao lado do código. */
    @Autowired
    public ColaboradoresSecao(ColaboradoresConsultas repo, FuncionarioRepository funcionarios) {
        this.repo = repo;
        this.funcionarios = funcionarios;
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
        Map<String, String> nomes = nomes();
        return repo.maisAtivos(empresa).stream().map(t -> mapa(
                "id", t.get("id"), "nome", nomes.get(String.valueOf(t.get("id"))), "cargo", t.get("cargo"), "senioridade", t.get("senioridade"),
                "area", t.get("area"), "req", inteiro(t.get("req")),
                "ferramentas", inteiro(t.get("ferramentas")), "dias", inteiro(t.get("dias")))).toList();
    }

    private Map<String, String> nomes() {
        if (funcionarios == null) {
            return Map.of();
        }
        Map<String, String> r = new HashMap<>();
        for (Funcionario f : funcionarios.findAll()) {
            if (f.getCodigo() != null && f.getNome() != null && !f.getNome().equals(f.getCodigo())) {
                r.put(f.getCodigo(), f.getNome());
            }
        }
        return r;
    }
}
