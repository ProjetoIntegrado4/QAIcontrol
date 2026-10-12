package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.service.ProdutividadeUseCase;
import br.edu.exemplo.ia.service.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa o controller do painel junto com a configuração de segurança real da aplicação. */
@WebMvcTest(ProdutividadeController.class)
@Import(SecurityConfig.class)
class ProdutividadeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProdutividadeUseCase useCase;

    @Test
    void semLoginDeveResponder401() throws Exception {
        mvc.perform(get("/api/produtividade/resumo"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Autenticacao necessaria\"}"));
    }

    @Test
    @WithMockUser
    void resumoDeveRetornarDadosDoCasoDeUso() throws Exception {
        when(useCase.resumo()).thenReturn(Map.of("atualizado_em", "12/10/2026 10:00"));

        mvc.perform(get("/api/produtividade/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizado_em").value("12/10/2026 10:00"));
    }

    @Test
    @WithMockUser
    void painelDeveRetornarIndicadoresDaEmpresa() throws Exception {
        when(useCase.painel("C01")).thenReturn(Map.of("kpis", Map.of("funcionarios", 10)));

        mvc.perform(get("/api/produtividade/empresas/C01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.funcionarios").value(10));
    }

    @Test
    @WithMockUser
    void empresaInexistenteDeveResponder404() throws Exception {
        when(useCase.painel("X99")).thenThrow(new NoSuchElementException("Empresa não encontrada: X99"));

        mvc.perform(get("/api/produtividade/empresas/X99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Empresa não encontrada: X99"));
    }

    @Test
    void healthDeveSerPublico() throws Exception {
        // Sem actuator neste slice, a rota não existe, mas a segurança não deve exigir login.
        mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
    }
}
