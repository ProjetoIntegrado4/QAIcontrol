package br.edu.exemplo.ia.repository.produtividade;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O que existe de fato no banco: tabelas e colunas do schema configurado, lidas de information_schema.
 * As seções do painel consultam este catálogo antes de rodar, então uma tabela ou coluna ausente
 * tira só a parte do painel que depende dela.
 */
@Component
public class CatalogoBanco {

    private final JdbcTemplate jdbc;
    private final EsquemaBase esquema;
    private volatile Map<String, Set<String>> colunas;

    public CatalogoBanco(JdbcTemplate jdbc, EsquemaBase esquema) {
        this.jdbc = jdbc;
        this.esquema = esquema;
    }

    /** Descarta o catálogo em memória; a próxima consulta relê o banco (ex.: depois de criar tabelas). */
    public void atualizar() {
        colunas = null;
    }

    private Map<String, Set<String>> colunas() {
        Map<String, Set<String>> c = colunas;
        if (c == null) {
            Map<String, Set<String>> lido = new HashMap<>();
            jdbc.query("select table_name, column_name from information_schema.columns where table_schema = ?",
                    rs -> { lido.computeIfAbsent(rs.getString(1), t -> new HashSet<>()).add(rs.getString(2)); },
                    esquema.schema());
            colunas = c = lido;
        }
        return c;
    }

    /**
     * Requisitos lógicos ("uso" para a tabela, "uso.status" para a coluna) que o banco não atende,
     * escritos com os nomes físicos. Lista vazia quando tudo existe.
     */
    public List<String> faltantes(Collection<String> requisitos) {
        Map<String, Set<String>> c = colunas();
        List<String> faltam = new ArrayList<>();
        for (String[] f : esquema.fisicos(requisitos)) {
            if (f[0] == null) faltam.add(f[1] + " (não mapeado em produtividade.yml)");
            else if (!c.containsKey(f[0])) faltam.add(f[0]);
            else if (f[1] != null && !c.get(f[0]).contains(f[1])) faltam.add(f[0] + "." + f[1]);
        }
        return faltam.stream().distinct().toList();
    }

    public boolean possui(String... requisitos) {
        return faltantes(List.of(requisitos)).isEmpty();
    }
}
