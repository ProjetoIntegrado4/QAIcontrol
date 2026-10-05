package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

public interface AuthUseCase {
    AuthResponse login(String username, String password, HttpServletRequest request, HttpServletResponse response);

    AuthResponse currentUser(Authentication authentication);
}