package br.edu.exemplo.ia.repository.csv;

/** Falha ao ler ou gravar um arquivo da base de dados em CSV. */
public class BaseDadosException extends RuntimeException {
    public BaseDadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
