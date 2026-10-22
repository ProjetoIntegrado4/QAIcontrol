package br.edu.exemplo.ia.service.risco;

import br.edu.exemplo.ia.config.RiscoProperties.Faixa;
import br.edu.exemplo.ia.dto.FaixaRiscoResponse;
import br.edu.exemplo.ia.dto.RecomendacaoRiscoResponse;
import br.edu.exemplo.ia.dto.RegrasRiscoResponse;
import br.edu.exemplo.ia.dto.UnidadeRiscoRequest;
import br.edu.exemplo.ia.dto.UnidadeRiscoResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Recomendações associadas ao risco de dependência de IA (Task PS-290 / RF27, US PP-75). A faixa e as
 * recomendações vêm sempre da {@link RegraRecomendacaoRisco}; aqui ficam só a leitura da entrada e a resposta.
 */
@Service
public class RecomendacaoRiscoService implements RecomendacaoRiscoUseCase {

    static final String DADOS_INSUFICIENTES = "Dados insuficientes para calcular o risco";

    private final RegraRecomendacaoRisco regra;

    public RecomendacaoRiscoService(RegraRecomendacaoRisco regra) {
        this.regra = regra;
    }

    @Override
    public RegrasRiscoResponse regras() {
        return new RegrasRiscoResponse(regra.versao(), regra.vigencia(), regra.criterio(),
                regra.faixas().stream().map(this::faixa).toList());
    }

    @Override
    public RecomendacaoRiscoResponse recomendar(String score) {
        Double valor = numero(score);
        String faixa = regra.classificar(valor);
        String mensagem = valor == null ? DADOS_INSUFICIENTES : null;
        return new RecomendacaoRiscoResponse(valor, faixa, regra.recomendacoes(faixa), regra.versao(), regra.vigencia(),
                mensagem);
    }

    @Override
    public List<UnidadeRiscoResponse> recomendarLote(List<UnidadeRiscoRequest> unidades) {
        if (unidades == null || unidades.isEmpty()) throw new IllegalArgumentException("Informe ao menos uma unidade");
        return unidades.stream().map(this::recomendarUnidade).toList();
    }

    private UnidadeRiscoResponse recomendarUnidade(UnidadeRiscoRequest unidade) {
        validar(unidade);
        String faixa = classificar(unidade);
        return new UnidadeRiscoResponse(unidade.unidade(), unidade.periodo(), unidade.score(), faixa,
                regra.recomendacoes(faixa), regra.versao());
    }

    private static void validar(UnidadeRiscoRequest unidade) {
        if (unidade == null || vazio(unidade.unidade())) throw new IllegalArgumentException("Unidade obrigatória");
        if (vazio(unidade.periodo())) {
            throw new IllegalArgumentException("Período obrigatório para a unidade " + unidade.unidade());
        }
    }

    /** Num lote, o erro diz qual unidade tem o score fora da escala. */
    private String classificar(UnidadeRiscoRequest unidade) {
        try {
            return regra.classificar(unidade.score());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unidade " + unidade.unidade() + ": " + e.getMessage());
        }
    }

    private FaixaRiscoResponse faixa(Faixa faixa) {
        return new FaixaRiscoResponse(faixa.nome(), faixa.limiteInferior(), faixa.limiteSuperior(),
                regra.limiteSuperiorInclusivo(faixa), regra.recomendacoes(faixa.nome()));
    }

    /** Score da query string; vazio vira nulo (dados insuficientes), nunca zero. */
    private static Double numero(String score) {
        if (vazio(score)) return null;
        try {
            return Double.valueOf(score.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Score inválido: informe um número de 0 a 100");
        }
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
