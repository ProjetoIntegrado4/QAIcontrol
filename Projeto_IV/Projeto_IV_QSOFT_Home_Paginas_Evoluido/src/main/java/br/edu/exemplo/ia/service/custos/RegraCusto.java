package br.edu.exemplo.ia.service.custos;

import br.edu.exemplo.ia.config.CustosProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Regra de cálculo de custos de IA (US PP-18). É o único lugar do código com a fórmula e com a
 * classificação de status cobrados e de agentes; os valores ficam no application.yml (seção custos).
 *
 * <p>custo por uso = input_tokens / 1000 × preço de entrada + output_tokens / 1000 × preço de saída,
 * com os preços por 1k tokens de ai_models, na moeda configurada (sem câmbio).
 * Como a fórmula é linear nos tokens, aplicá-la à soma dos tokens de um mesmo modelo dá o mesmo
 * resultado que somar o custo uso a uso; por isso o banco só soma os tokens.
 *
 * <p>PRELIMINAR – validar com o PO: moeda/câmbio, status cobrados e lista de aplicações de agente.
 */
@Component
public class RegraCusto {

    private static final int CASAS = 4;

    private final CustosProperties props;

    public RegraCusto(CustosProperties props) {
        this.props = props;
    }

    /**
     * Custo de um conjunto de usos do mesmo modelo. Sem preço de entrada ou de saída devolve nulo,
     * e não zero: ausência de informação não é custo zero (RN19).
     */
    public BigDecimal custo(long tokensEntrada, long tokensSaida, BigDecimal precoEntrada, BigDecimal precoSaida) {
        if (precoEntrada == null || precoSaida == null) return null;
        return BigDecimal.valueOf(tokensEntrada).movePointLeft(3).multiply(precoEntrada)
                .add(BigDecimal.valueOf(tokensSaida).movePointLeft(3).multiply(precoSaida));
    }

    public boolean cobrado(String status) {
        return status != null && props.statusCobrados().contains(status);
    }

    public boolean agente(String aplicacao) {
        return aplicacao != null && props.aplicacoesAgente().contains(aplicacao);
    }

    /** Valor exibido: {@value #CASAS} casas decimais, arredondamento comercial. */
    public BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS, RoundingMode.HALF_UP);
    }

    public String formula() {
        return "custo (" + moeda() + ") = input_tokens / 1000 × preço de entrada por 1k tokens"
                + " + output_tokens / 1000 × preço de saída por 1k tokens (ai_models), somado por uso com status em "
                + props.statusCobrados() + "; usos de modelo sem preço não entram no total";
    }

    public List<String> statusCobrados() {
        return props.statusCobrados();
    }

    public List<String> aplicacoesAgente() {
        return props.aplicacoesAgente();
    }

    public String versao() {
        return props.versaoRegra();
    }

    public String vigencia() {
        return props.vigencia();
    }

    public String moeda() {
        return props.moeda();
    }

    public int limiteColaboradores() {
        return props.limiteColaboradores();
    }
}
