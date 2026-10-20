package br.edu.exemplo.ia.domain.entity;

import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CertificacaoTest {

    private static Funcionario funcionario() {
        return new Funcionario("Ana", new Empresa(new EmpresaVO("QSoft"), "Tecnologia"), "Analista");
    }

    @Test
    void deveCriarCertificacaoComDataDeCriacao() {
        Funcionario funcionario = funcionario();
        Certificacao c = new Certificacao(funcionario, "  Fundamentos de IA ", " Alura ",
                LocalDate.of(2026, 5, 10), TipoCertificacao.CURSO);

        assertNotNull(c.getId());
        assertSame(funcionario, c.getFuncionario());
        assertEquals("Fundamentos de IA", c.getNome());
        assertEquals("Alura", c.getInstituicao());
        assertEquals(LocalDate.of(2026, 5, 10), c.getDataConclusao());
        assertEquals(TipoCertificacao.CURSO, c.getTipo());
        assertNotNull(c.getDataCriacao());
    }

    @Test
    void deveRejeitarCamposObrigatorios() {
        LocalDate data = LocalDate.of(2026, 5, 10);
        assertThrows(IllegalArgumentException.class,
                () -> new Certificacao(null, "Curso", "Alura", data, TipoCertificacao.CURSO));
        assertThrows(IllegalArgumentException.class,
                () -> new Certificacao(funcionario(), null, "Alura", data, TipoCertificacao.CURSO));
        assertThrows(IllegalArgumentException.class,
                () -> new Certificacao(funcionario(), "Curso", null, data, TipoCertificacao.CURSO));
        assertThrows(IllegalArgumentException.class,
                () -> new Certificacao(funcionario(), "Curso", "Alura", null, TipoCertificacao.CURSO));
        assertThrows(IllegalArgumentException.class,
                () -> new Certificacao(funcionario(), "Curso", "Alura", data, null));
    }

    @Test
    void tipoDeveAceitarTextoEmMinusculas() {
        assertEquals(TipoCertificacao.CERTIFICACAO, TipoCertificacao.converter(" certificacao "));
        assertEquals(TipoCertificacao.CURSO, TipoCertificacao.converter("CURSO"));
        assertThrows(IllegalArgumentException.class, () -> TipoCertificacao.converter(null));
        assertThrows(IllegalArgumentException.class, () -> TipoCertificacao.converter("WORKSHOP"));
    }
}
