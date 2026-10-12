package br.edu.exemplo.ia.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void argumentoInvalidoDeveVirarBadRequest() {
        ResponseEntity<Map<String, Object>> r = handler.handleIllegalArgument(new IllegalArgumentException("CPF inválido"));

        assertEquals(400, r.getStatusCode().value());
        assertEquals("CPF inválido", r.getBody().get("message"));
        assertEquals("Bad Request", r.getBody().get("error"));
        assertNotNull(r.getBody().get("timestamp"));
    }

    @Test
    void falhaDeAutenticacaoDeveVirarUnauthorized() {
        ResponseEntity<Map<String, Object>> r = handler.handleAuthenticationFailure(new BadCredentialsException("x"));

        assertEquals(401, r.getStatusCode().value());
        assertEquals("Usuário ou senha inválidos", r.getBody().get("message"));
    }

    @Test
    void erroDeValidacaoDeveInformarOPrimeiroCampo() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "nome", "não deve estar em branco"));

        ResponseEntity<Map<String, Object>> r = handler.handleValidation(excecao(binding));

        assertEquals(400, r.getStatusCode().value());
        assertEquals("nome: não deve estar em branco", r.getBody().get("message"));
    }

    @Test
    void erroDeValidacaoSemCampoDeveUsarMensagemPadrao() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");

        ResponseEntity<Map<String, Object>> r = handler.handleValidation(excecao(binding));

        assertEquals("Dados inválidos", r.getBody().get("message"));
    }

    private static MethodArgumentNotValidException excecao(BeanPropertyBindingResult binding) throws Exception {
        MethodParameter parametro = new MethodParameter(
                ApiExceptionHandlerTest.class.getDeclaredMethod("excecao", BeanPropertyBindingResult.class), 0);
        return new MethodArgumentNotValidException(parametro, binding);
    }
}
