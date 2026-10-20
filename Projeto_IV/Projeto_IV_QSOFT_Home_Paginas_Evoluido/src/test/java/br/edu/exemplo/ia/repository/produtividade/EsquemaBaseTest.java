package br.edu.exemplo.ia.repository.produtividade;

import br.edu.exemplo.ia.config.ProdutividadeProperties;
import br.edu.exemplo.ia.config.ProdutividadeTestes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EsquemaBaseTest {

    private final EsquemaBase esquema = new EsquemaBase(ProdutividadeTestes.propriedades());

    @Test
    void sqlDeveTrocarNomesLogicosPelosFisicos() {
        assertEquals("select l.\"employee_id\" from \"public\".\"ai_usage_logs\" l where l.\"timestamp\" is not null",
                esquema.sql("select l.${uso.funcionario} from ${uso} l where l.${uso.data} is not null"));
    }

    @Test
    void referenciaNaoMapeadaDeveFalhar() {
        assertThrows(IllegalArgumentException.class, () -> esquema.sql("select 1 from ${inexistente}"));
        assertThrows(IllegalArgumentException.class, () -> esquema.sql("select ${uso.inexistente} from ${uso}"));
    }

    @Test
    void parametrosDevemTrazerEmpresaJanelaEValores() {
        MapSqlParameterSource p = esquema.parametros("C01");
        assertEquals("C01", p.getValue("emp"));
        assertEquals(90, p.getValue("janela_dias"));
        assertEquals(12, p.getValue("limite_ranking"));
        assertEquals("CC-", p.getValue("centro_custo_prefixo"));
        assertEquals(List.of("success"), p.getValue("uso_sucesso"));
        assertEquals(List.of("Alta", "Critica"), p.getValue("seguranca_grave"));
    }

    @Test
    void listaDeValoresVaziaNaoGeraSqlInvalido() {
        ProdutividadeProperties props = new ProdutividadeProperties("public", "x", 90, 12, "", Map.of(),
                Map.of("uso-sucesso", List.of()));
        assertEquals(List.of(""), new EsquemaBase(props).parametros("ALL").getValue("uso_sucesso"));
    }

    @Test
    void requisitosDevemVirarNomesFisicos() {
        List<String[]> f = esquema.fisicos(List.of("uso", "uso.status", "nada.x"));
        assertArrayEquals(new String[]{"ai_usage_logs", null}, f.get(0));
        assertArrayEquals(new String[]{"ai_usage_logs", "request_status"}, f.get(1));
        assertArrayEquals(new String[]{null, "nada.x"}, f.get(2));
    }

    @Test
    void expoeMapeamentoEValores() {
        assertEquals("ai_billing", esquema.tabelasMapeadas().get("faturamento"));
        assertEquals(List.of("Concluido"), esquema.valores("treinamento-concluido"));
        assertEquals(List.of(), esquema.valores("nao-existe"));
        assertTrue(esquema.todosValores().containsKey("pr-mergeado"));
        assertEquals(90, esquema.janelaRecenteDias());
        assertEquals("public", esquema.schema());
    }
}
