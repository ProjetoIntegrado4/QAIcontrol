package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.FaixaRiscoResponse;
import br.edu.exemplo.ia.dto.RecomendacaoRiscoResponse;
import br.edu.exemplo.ia.dto.RegrasRiscoResponse;
import br.edu.exemplo.ia.dto.UnidadeRiscoRequest;
import br.edu.exemplo.ia.dto.UnidadeRiscoResponse;
import br.edu.exemplo.ia.service.SecurityConfig;
import br.edu.exemplo.ia.service.risco.RecomendacaoRiscoUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recomendações por faixa de risco (PS-290 / RF27) com a segurança real da aplicação e o ApiExceptionHandler. */
@WebMvcTest(RecomendacaoRiscoController.class)
@Import(SecurityConfig.class)
class RecomendacaoRiscoControllerTest {

    private static final String VERSAO = "risco-recomendacoes-v1-preliminar";
    private static final List<String> RECOMENDACOES_ALTO = List.of(
            "Gestor revisar com a equipe o processo em que a IA é utilizada.",
            "Exigir registro de revisão humana nas entregas com IA.");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecomendacaoRiscoUseCase useCase;

    @Test
    void semLoginNaoDeveRecomendar() throws Exception {
        mvc.perform(get("/api/risco/recomendacoes").param("score", "60"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Autenticacao necessaria\"}"));
        verifyNoInteractions(useCase);
    }

    @Test
    void semLoginNaoDeveMostrarAsRegras() throws Exception {
        mvc.perform(get("/api/risco/recomendacoes/regras"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semLoginNaoDeveRecomendarEmLote() throws Exception {
        mvc.perform(post("/api/risco/recomendacoes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser
    void recomendarDeveRepassarOScoreEResponder200() throws Exception {
        when(useCase.recomendar("60")).thenReturn(
                new RecomendacaoRiscoResponse(60.0, "ALTO", RECOMENDACOES_ALTO, VERSAO, "2026-10-22", null));

        mvc.perform(get("/api/risco/recomendacoes").param("score", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(60.0))
                .andExpect(jsonPath("$.faixa").value("ALTO"))
                .andExpect(jsonPath("$.recomendacoes[0]").value(RECOMENDACOES_ALTO.get(0)))
                .andExpect(jsonPath("$.versaoRegra").value(VERSAO))
                .andExpect(jsonPath("$.vigencia").value("2026-10-22"));
        verify(useCase).recomendar("60");
    }

    @Test
    @WithMockUser
    void recomendarSemScoreDeveRepassarNulo() throws Exception {
        when(useCase.recomendar(null)).thenReturn(new RecomendacaoRiscoResponse(null, "DADOS_INSUFICIENTES", List.of(),
                VERSAO, "2026-10-22", "Dados insuficientes para calcular o risco"));

        mvc.perform(get("/api/risco/recomendacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faixa").value("DADOS_INSUFICIENTES"))
                .andExpect(jsonPath("$.recomendacoes").isEmpty())
                .andExpect(jsonPath("$.mensagem").value("Dados insuficientes para calcular o risco"));
    }

    @Test
    @WithMockUser
    void scoreForaDaEscalaDeveResponder400ComMensagem() throws Exception {
        when(useCase.recomendar("101")).thenThrow(new IllegalArgumentException("Score deve estar entre 0 e 100"));

        mvc.perform(get("/api/risco/recomendacoes").param("score", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Score deve estar entre 0 e 100"));
    }

    @Test
    @WithMockUser
    void regrasDevemResponder200ComVersaoVigenciaEFaixas() throws Exception {
        when(useCase.regras()).thenReturn(new RegrasRiscoResponse(VERSAO, "2026-10-22", "critério",
                List.of(new FaixaRiscoResponse("ALTO", 50.0, 75.0, false, RECOMENDACOES_ALTO))));

        mvc.perform(get("/api/risco/recomendacoes/regras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versaoRegra").value(VERSAO))
                .andExpect(jsonPath("$.vigencia").value("2026-10-22"))
                .andExpect(jsonPath("$.faixas[0].nome").value("ALTO"))
                .andExpect(jsonPath("$.faixas[0].limiteInferior").value(50.0))
                .andExpect(jsonPath("$.faixas[0].limiteSuperior").value(75.0))
                .andExpect(jsonPath("$.faixas[0].recomendacoes.length()").value(2));
    }

    @Test
    @WithMockUser
    void loteDeveRepassarAsUnidadesEResponder200() throws Exception {
        List<UnidadeRiscoRequest> pedido = List.of(new UnidadeRiscoRequest("C01-E0001", "2026-07", 60.0),
                new UnidadeRiscoRequest("C01-E0002", "2026-07", null));
        when(useCase.recomendarLote(pedido)).thenReturn(List.of(
                new UnidadeRiscoResponse("C01-E0001", "2026-07", 60.0, "ALTO", RECOMENDACOES_ALTO, VERSAO),
                new UnidadeRiscoResponse("C01-E0002", "2026-07", null, "DADOS_INSUFICIENTES", List.of(), VERSAO)));

        mvc.perform(post("/api/risco/recomendacoes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"unidade":"C01-E0001","periodo":"2026-07","score":60},
                                 {"unidade":"C01-E0002","periodo":"2026-07","score":null}]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].faixa").value("ALTO"))
                .andExpect(jsonPath("$[0].versaoRegra").value(VERSAO))
                .andExpect(jsonPath("$[1].faixa").value("DADOS_INSUFICIENTES"))
                .andExpect(jsonPath("$[1].recomendacoes").isEmpty());
        verify(useCase).recomendarLote(pedido);
    }

    @Test
    @WithMockUser
    void loteInvalidoDeveResponder400ComMensagem() throws Exception {
        when(useCase.recomendarLote(List.of())).thenThrow(new IllegalArgumentException("Informe ao menos uma unidade"));

        mvc.perform(post("/api/risco/recomendacoes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Informe ao menos uma unidade"));
    }
}
