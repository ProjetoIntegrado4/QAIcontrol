package br.edu.exemplo.ia.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void deveCriarUsuarioTemporarioComSenhaCriptografada() {
        PasswordEncoder encoder = config.passwordEncoder();

        UserDetails user = config.userDetailsService(encoder, "admin@qsoft.com", "Qsoft123!")
                .loadUserByUsername("admin@qsoft.com");

        assertTrue(encoder.matches("Qsoft123!", user.getPassword()));
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void deveExporAuthenticationManagerDaConfiguracao() throws Exception {
        AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(configuration.getAuthenticationManager()).thenReturn(manager);

        assertSame(manager, config.authenticationManager(configuration));
    }

    @Test
    void deveGuardarContextoNaSessaoECsrfEmCookie() {
        assertInstanceOf(HttpSessionSecurityContextRepository.class, config.securityContextRepository());
        CookieCsrfTokenRepository csrf = config.csrfTokenRepository();
        assertNotNull(csrf);
    }
}
