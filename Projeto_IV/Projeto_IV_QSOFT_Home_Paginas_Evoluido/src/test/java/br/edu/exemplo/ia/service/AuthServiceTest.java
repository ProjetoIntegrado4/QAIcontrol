package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devePersistirSessaoAposLoginValido() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        SecurityContextRepository contextRepository = mock(SecurityContextRepository.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            "admin@qsoft.com", "credential", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        AuthService service = new AuthService(authenticationManager, contextRepository, "Administrador QSOFT");
        AuthResponse result = service.login("admin@qsoft.com", "Qsoft123!", request, response);

        assertEquals(new AuthResponse("Administrador QSOFT", "admin@qsoft.com", "ADMIN"), result);
        verify(contextRepository).saveContext(any(), org.mockito.ArgumentMatchers.same(request),
                org.mockito.ArgumentMatchers.same(response));
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
    }
}