package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.AceiteResponse;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.dto.UsageRequest;
import br.edu.exemplo.ia.dto.UsageResponse;
import br.edu.exemplo.ia.service.AceitePendenteException;
import br.edu.exemplo.ia.service.NaoEncontradoException;
import br.edu.exemplo.ia.service.PoliticaUseCase;
import br.edu.exemplo.ia.service.SecurityConfig;
import br.edu.exemplo.ia.service.UsageUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Políticas de uso de IA (US PP-21) com a segurança real da aplicação e o ApiExceptionHandler.
 * Inclui o UsageController para o bloqueio do registro de uso sem aceite (BDD 2).
 */
@WebMvcTest({PoliticaController.class, UsageController.class})
@Import(SecurityConfig.class)
class PoliticaControllerTest {

    private static final UUID EMPRESA = UUID.randomUUID();
    private static final UUID FUNCIONARIO = UUID.randomUUID();
    private static final UUID CODIGO = UUID.randomUUID();
    private static final UUID POLITICA = UUID.randomUUID();
    private static final String CORPO_POLITICA = """
            {"empresaId":"%s","titulo":"Uso responsável de IA","conteudo":"Não insira dados pessoais.","obrigatoria":true}
            """.formatted(EMPRESA);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PoliticaUseCase useCase;

    @MockitoBean
    private UsageUseCase usages;

    private static PoliticaResponse politica(int versao) {
        return new PoliticaResponse(POLITICA, CODIGO, EMPRESA, "QSoft", "Uso responsável de IA",
                "Não insira dados pessoais.", versao, true, true, Instant.parse("2026-10-22T12:00:00Z"));
    }

    // CA-14.1: sem login não publica
    @Test
    void semLoginNaoPublicaPoliticaE401() throws Exception {
        mvc.perform(post("/api/politicas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Autenticacao necessaria\"}"));
        verifyNoInteractions(useCase);
    }

    @Test
    void semLoginNaoConsultaPoliticas() throws Exception {
        mvc.perform(get("/api/politicas").param("empresaId", EMPRESA.toString()))
                .andExpect(status().isUnauthorized());
    }

    // CA-14.1 / RN04: usuário autenticado sem perfil Administrador não publica nem altera
    @Test
    @WithMockUser(roles = "COLABORADOR")
    void usuarioSemPerfilAdminNaoPublicaNemAlteraPolitica() throws Exception {
        mvc.perform(post("/api/politicas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso restrito ao perfil Administrador"));
        mvc.perform(put("/api/politicas/" + CODIGO).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void requisicaoSemCsrfDeveResponder403ComMensagem() throws Exception {
        mvc.perform(post("/api/politicas").contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Token CSRF invalido ou ausente"));
    }

    // BDD 1: admin publica a política vinculada à empresa
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPublicaPoliticaE200() throws Exception {
        PoliticaRequest pedido = new PoliticaRequest(EMPRESA, "Uso responsável de IA", "Não insira dados pessoais.", true);
        when(useCase.publicar(pedido)).thenReturn(politica(1));

        mvc.perform(post("/api/politicas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(CODIGO.toString()))
                .andExpect(jsonPath("$.empresaId").value(EMPRESA.toString()))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.obrigatoria").value(true))
                .andExpect(jsonPath("$.vigente").value(true));
        verify(useCase).publicar(pedido);
    }

    // CA-14.3: PUT publica a próxima versão
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPublicaNovaVersaoE200() throws Exception {
        when(useCase.novaVersao(any(), any())).thenReturn(politica(2));

        mvc.perform(put("/api/politicas/" + CODIGO).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2));
        verify(useCase).novaVersao(CODIGO, new PoliticaRequest(EMPRESA, "Uso responsável de IA", "Não insira dados pessoais.", true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validacaoDoCasoDeUsoDeveResponder400ComMensagem() throws Exception {
        when(useCase.publicar(any())).thenThrow(new IllegalArgumentException("Título da política obrigatório"));

        mvc.perform(post("/api/politicas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Título da política obrigatório"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void empresaInexistenteDeveResponder404ComMensagem() throws Exception {
        when(useCase.publicar(any())).thenThrow(new NaoEncontradoException("Empresa inexistente: " + EMPRESA));

        mvc.perform(post("/api/politicas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CORPO_POLITICA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Empresa inexistente: " + EMPRESA));
    }

    // CA-14.2 / CA-15.1: consultas liberadas para qualquer usuário autenticado
    @Test
    @WithMockUser
    void consultasDevemResponder200() throws Exception {
        when(useCase.vigentes(EMPRESA)).thenReturn(List.of(politica(1)));
        when(useCase.versoes(CODIGO)).thenReturn(List.of(politica(2), politica(1)));
        when(useCase.pendentes(EMPRESA, FUNCIONARIO)).thenReturn(List.of(politica(1)));

        mvc.perform(get("/api/politicas").param("empresaId", EMPRESA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Uso responsável de IA"));
        mvc.perform(get("/api/politicas/" + CODIGO + "/versoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versao").value(2))
                .andExpect(jsonPath("$[1].versao").value(1));
        mvc.perform(get("/api/politicas/pendentes").param("empresaId", EMPRESA.toString())
                        .param("funcionarioId", FUNCIONARIO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conteudo").value("Não insira dados pessoais."));
    }

    // CA-15.2 / CA-15.3 / CA-15.4: o colaborador (sem perfil admin) registra e consulta os aceites
    @Test
    @WithMockUser
    void colaboradorRegistraEConsultaAceites() throws Exception {
        AceiteResponse aceite = new AceiteResponse(UUID.randomUUID(), FUNCIONARIO, "Ana Souza", POLITICA, CODIGO,
                "Uso responsável de IA", 1, Instant.parse("2026-10-22T12:30:00Z"));
        when(useCase.aceitar(new AceiteRequest(FUNCIONARIO, POLITICA))).thenReturn(aceite);
        when(useCase.aceites(FUNCIONARIO)).thenReturn(List.of(aceite));

        mvc.perform(post("/api/politicas/aceites").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"funcionarioId\":\"%s\",\"politicaId\":\"%s\"}".formatted(FUNCIONARIO, POLITICA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioId").value(FUNCIONARIO.toString()))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.dataAceite").value("2026-10-22T12:30:00Z"));
        mvc.perform(get("/api/politicas/aceites").param("funcionarioId", FUNCIONARIO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versao").value(1));
    }

    // BDD 2: registro de uso bloqueado responde 409 com a mensagem e as políticas pendentes
    @Test
    @WithMockUser
    void usoSemAceiteDeveResponder409ComPendencias() throws Exception {
        when(usages.register(any())).thenThrow(new AceitePendenteException(List.of(politica(1))));

        mvc.perform(post("/api/usages").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":\"%s\",\"tokens\":500,\"model\":\"gpt-demo\",\"funcionarioId\":\"%s\"}"
                                .formatted(EMPRESA, FUNCIONARIO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Aceite da política de uso de IA obrigatório"))
                .andExpect(jsonPath("$.pendentes[0].titulo").value("Uso responsável de IA"))
                .andExpect(jsonPath("$.pendentes[0].versao").value(1));
        verify(usages).register(new UsageRequest(EMPRESA, 500, "gpt-demo", FUNCIONARIO));
    }

    // Retrocompatibilidade: o corpo antigo do registro de uso (sem funcionarioId) continua aceito
    @Test
    @WithMockUser
    void usoSemFuncionarioContinuaFuncionando() throws Exception {
        when(usages.register(new UsageRequest(EMPRESA, 500, "gpt-demo"))).thenReturn(
                new UsageResponse(UUID.randomUUID(), EMPRESA, 500, "gpt-demo", Instant.now(), "INFO", "ok"));

        mvc.perform(post("/api/usages").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":\"%s\",\"tokens\":500,\"model\":\"gpt-demo\"}".formatted(EMPRESA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertLevel").value("INFO"));
    }
}
