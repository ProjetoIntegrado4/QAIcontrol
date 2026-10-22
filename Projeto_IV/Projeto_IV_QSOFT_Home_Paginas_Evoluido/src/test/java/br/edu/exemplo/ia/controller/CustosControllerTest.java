package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.CustoItemResponse;
import br.edu.exemplo.ia.dto.CustoOpcoesResponse;
import br.edu.exemplo.ia.dto.CustoRequest;
import br.edu.exemplo.ia.dto.CustoResponse;
import br.edu.exemplo.ia.dto.OpcaoCustoResponse;
import br.edu.exemplo.ia.service.SecurityConfig;
import br.edu.exemplo.ia.service.custos.CustosUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller de custos de IA (US PP-18) com a segurança real da aplicação e o ApiExceptionHandler. */
@WebMvcTest(CustosController.class)
@Import(SecurityConfig.class)
class CustosControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustosUseCase useCase;

    private static CustoResponse resposta() {
        CustoItemResponse dep = new CustoItemResponse("C01-D02", "DevOps e Infraestrutura", new BigDecimal("0.1050"), 1, 0);
        return new CustoResponse(new BigDecimal("0.1400"), "USD", 3, 0, 3000, 3000,
                new BigDecimal("0.1050"), new BigDecimal("0.0350"), List.of(dep), List.of(), List.of(),
                "custos-v1-preliminar", "2026-10-22", "custo = ...", List.of("success"), List.of("API Direta"),
                null, Map.of());
    }

    @Test
    void semLoginDeveResponder401() throws Exception {
        mvc.perform(get("/api/custos").param("empresaId", "C01"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Autenticacao necessaria\"}"));
    }

    @Test
    @WithMockUser
    void calcularDeveRepassarOsFiltrosEResponder200() throws Exception {
        CustoRequest pedido = new CustoRequest("C01", "2026-07-01", "2026-07-31", "C01-D02", null);
        when(useCase.calcular(pedido)).thenReturn(resposta());

        mvc.perform(get("/api/custos").param("empresaId", "C01").param("inicio", "2026-07-01")
                        .param("fim", "2026-07-31").param("departamentoId", "C01-D02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0.14))
                .andExpect(jsonPath("$.moeda").value("USD"))
                .andExpect(jsonPath("$.totalAgentes").value(0.105))
                .andExpect(jsonPath("$.porDepartamento[0].nome").value("DevOps e Infraestrutura"))
                .andExpect(jsonPath("$.versaoRegra").value("custos-v1-preliminar"))
                .andExpect(jsonPath("$.vigencia").value("2026-10-22"));
        verify(useCase).calcular(pedido);
    }

    @Test
    @WithMockUser
    void validacaoDoCasoDeUsoDeveResponder400ComMensagem() throws Exception {
        when(useCase.calcular(any())).thenThrow(
                new IllegalArgumentException("Data de início deve ser anterior ou igual à data de fim"));

        mvc.perform(get("/api/custos").param("empresaId", "C01").param("inicio", "2026-08-01").param("fim", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Data de início deve ser anterior ou igual à data de fim"));
    }

    @Test
    @WithMockUser
    void opcoesDevemResponder200() throws Exception {
        when(useCase.opcoes("C01")).thenReturn(new CustoOpcoesResponse(
                List.of(new OpcaoCustoResponse("C01", "NexoCode Solucoes", null)), List.of(), List.of(),
                LocalDate.of(2025, 6, 1), LocalDate.of(2026, 7, 31), Map.of()));

        mvc.perform(get("/api/custos/opcoes").param("empresaId", "C01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresas[0].nome").value("NexoCode Solucoes"))
                .andExpect(jsonPath("$.dataMaxima").value("2026-07-31"));
    }

    @Test
    @WithMockUser
    void opcoesSemEmpresaDevemFuncionar() throws Exception {
        when(useCase.opcoes(null)).thenReturn(new CustoOpcoesResponse(List.of(), List.of(), List.of(), null, null,
                Map.of("opcoes", "faltam no banco: companies")));

        mvc.perform(get("/api/custos/opcoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indisponiveis.opcoes").value("faltam no banco: companies"));
    }
}
