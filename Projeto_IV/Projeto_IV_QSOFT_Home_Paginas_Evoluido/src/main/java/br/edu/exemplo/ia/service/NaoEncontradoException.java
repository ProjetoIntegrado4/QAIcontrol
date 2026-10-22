package br.edu.exemplo.ia.service;

/** Registro informado não existe. O ApiExceptionHandler responde 404 com a mensagem. */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String message) {
        super(message);
    }
}
