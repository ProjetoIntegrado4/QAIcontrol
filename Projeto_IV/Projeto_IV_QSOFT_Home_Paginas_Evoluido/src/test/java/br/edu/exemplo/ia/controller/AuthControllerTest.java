package br.edu.exemplo.ia.controller;

import br.edu.exemplo.ia.dto.AuthRequest;
import br.edu.exemplo.ia.dto.AuthResponse;
import br.edu.exemplo.ia.service.AuthUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthUseCase auth = mock(AuthUseCase.class);
    private final AuthController controller = new AuthController(auth);

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void csrfDeveResponderSemConteudo() {
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "abc");

        assertEquals(204, controller.csrf(token).getStatusCode().value());
    }

    @Test
    void loginDeveRepassarUsuarioESenha() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse esperado = new AuthResponse("Administrador", "admin@qsoft.com", "ADMIN");
        when(auth.login("admin@qsoft.com", "Qsoft123!", request, response)).thenReturn(esperado);

        AuthResponse resultado = controller.login(new AuthRequest("admin@qsoft.com", "Qsoft123!"), request, response);

        assertSame(esperado, resultado);
    }

    @Test
    void meDeveRetornarUsuarioAtual() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "x", List.of());
        AuthResponse esperado = new AuthResponse("Administrador", "admin", "ADMIN");
        when(auth.currentUser(authentication)).thenReturn(esperado);

        assertSame(esperado, controller.currentUser(authentication));
    }

    @Test
    void logoutDeveInvalidarSessaoELimparContexto() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var resposta = controller.logout(request, new MockHttpServletResponse(), authentication);

        assertEquals(204, resposta.getStatusCode().value());
        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
