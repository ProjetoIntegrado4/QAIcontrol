package br.edu.exemplo.ia.service.risco;

import br.edu.exemplo.ia.config.RiscoProperties;
import br.edu.exemplo.ia.config.RiscoProperties.Faixa;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regra de recomendações por faixa de risco de dependência de IA (Task PS-290 / RF27, US PP-75). É o único lugar
 * do código que classifica o Risk Score em faixa e escolhe as recomendações; os valores ficam no application.yml
 * (seção risco). A configuração é validada no construtor: se estiver inválida, a aplicação não sobe.
 *
 * <p>O Risk Score (0 a 100) ainda é calculado só no notebook 08_Analise_Preditiva; aqui ele chega pronto.
 * O resultado é apoio à decisão do gestor, não diagnóstico nem punição.
 *
 * <p>PRELIMINAR – validar com o PO: limites das faixas e textos das recomendações.
 */
@Component
public class RegraRecomendacaoRisco {

    /** Classificação de quem não tem score. Não é faixa configurável e não tem recomendação (RN19). */
    public static final String DADOS_INSUFICIENTES = "DADOS_INSUFICIENTES";

    private static final double ESCALA_MINIMA = 0;
    private static final double ESCALA_MAXIMA = 100;

    private final RiscoProperties props;
    private final List<Faixa> faixas;

    public RegraRecomendacaoRisco(RiscoProperties props) {
        validarIdentificacao(props);
        validarFaixas(props.faixas());
        List<Faixa> ordenadas = props.faixas().stream().sorted(Comparator.comparing(Faixa::limiteInferior)).toList();
        validarCobertura(ordenadas);
        validarRecomendacoes(ordenadas, props.recomendacoes());
        this.props = props;
        this.faixas = ordenadas;
    }

    /** Nome da faixa do score, ou {@value #DADOS_INSUFICIENTES} quando não há score. */
    public String classificar(Double score) {
        // RN19 / CA-23.3: score ausente não vira zero; a unidade fica sem faixa e sem recomendação.
        if (score == null) return DADOS_INSUFICIENTES;
        if (score.isNaN() || score < ESCALA_MINIMA || score > ESCALA_MAXIMA) {
            throw new IllegalArgumentException("Score deve estar entre 0 e 100");
        }
        return faixas.stream().filter(f -> contem(f, score)).findFirst().orElseThrow().nome();
    }

    /** Recomendações da faixa; vazia para {@value #DADOS_INSUFICIENTES}. */
    public List<String> recomendacoes(String faixa) {
        return List.copyOf(props.recomendacoes().getOrDefault(faixa, List.of()));
    }

    /** Só a última faixa inclui o limite superior (o 100). */
    public boolean limiteSuperiorInclusivo(Faixa faixa) {
        return faixa.limiteSuperior() == ESCALA_MAXIMA;
    }

    public String criterio() {
        return "Faixa pelo Risk Score de 0 a 100: limite inferior inclusivo e superior exclusivo; a última faixa"
                + " inclui o 100. Score ausente: Dados insuficientes (" + DADOS_INSUFICIENTES + "), sem recomendação"
                + " (RN19). Apoio à decisão do gestor, não diagnóstico.";
    }

    /** Faixas em ordem crescente de score. */
    public List<Faixa> faixas() {
        return faixas;
    }

    public String versao() {
        return props.versaoRegra();
    }

    public String vigencia() {
        return props.vigencia();
    }

    private boolean contem(Faixa faixa, double score) {
        return score >= faixa.limiteInferior() && (score < faixa.limiteSuperior() || limiteSuperiorInclusivo(faixa));
    }

    // Validação da configuração (na subida)

    private static void validarIdentificacao(RiscoProperties props) {
        if (props.versaoRegra() == null || props.versaoRegra().isBlank()) {
            throw invalida("informe risco.versao-regra (RN17)");
        }
        try {
            LocalDate.parse(props.vigencia() == null ? "" : props.vigencia());
        } catch (DateTimeParseException e) {
            throw invalida("risco.vigencia deve ser uma data AAAA-MM-DD (RN17)");
        }
    }

    private static void validarFaixas(List<Faixa> faixas) {
        if (faixas.isEmpty()) throw invalida("nenhuma faixa configurada em risco.faixas");
        Set<String> nomes = new HashSet<>();
        for (Faixa faixa : faixas) {
            validarFaixa(faixa);
            if (!nomes.add(faixa.nome())) throw invalida("faixa repetida: " + faixa.nome());
        }
    }

    private static void validarFaixa(Faixa faixa) {
        if (faixa.nome() == null || faixa.nome().isBlank()) throw invalida("faixa sem nome");
        if (DADOS_INSUFICIENTES.equals(faixa.nome())) {
            throw invalida(DADOS_INSUFICIENTES + " é reservado para score ausente e não pode ser faixa");
        }
        if (faixa.limiteInferior() == null || faixa.limiteSuperior() == null) {
            throw invalida("faixa " + faixa.nome() + " sem limite inferior ou superior");
        }
        if (faixa.limiteInferior() >= faixa.limiteSuperior()) {
            throw invalida("faixa " + faixa.nome() + " com limite inferior maior ou igual ao superior");
        }
    }

    /** As faixas, em ordem, precisam cobrir de 0 a 100 sem buraco nem sobreposição. */
    private static void validarCobertura(List<Faixa> ordenadas) {
        if (ordenadas.get(0).limiteInferior() != ESCALA_MINIMA) throw invalida("as faixas devem começar em 0");
        for (int i = 1; i < ordenadas.size(); i++) {
            Faixa anterior = ordenadas.get(i - 1);
            Faixa atual = ordenadas.get(i);
            String entre = " entre " + anterior.nome() + " e " + atual.nome();
            if (atual.limiteInferior() > anterior.limiteSuperior()) throw invalida("buraco" + entre);
            if (atual.limiteInferior() < anterior.limiteSuperior()) throw invalida("sobreposição" + entre);
        }
        if (ordenadas.get(ordenadas.size() - 1).limiteSuperior() != ESCALA_MAXIMA) {
            throw invalida("as faixas devem terminar em 100");
        }
    }

    /** Toda recomendação pertence a uma faixa existente e toda faixa tem ao menos uma recomendação (CA-27.1). */
    private static void validarRecomendacoes(List<Faixa> faixas, Map<String, List<String>> recomendacoes) {
        Set<String> nomes = new HashSet<>(faixas.stream().map(Faixa::nome).toList());
        for (String faixa : recomendacoes.keySet()) {
            if (!nomes.contains(faixa)) throw invalida("recomendação para faixa inexistente: " + faixa);
        }
        for (String faixa : nomes) {
            List<String> textos = recomendacoes.getOrDefault(faixa, List.of());
            if (textos.isEmpty()) throw invalida("faixa " + faixa + " sem recomendação");
            if (textos.stream().anyMatch(t -> t == null || t.isBlank())) {
                throw invalida("faixa " + faixa + " com recomendação em branco");
            }
        }
    }

    private static IllegalStateException invalida(String motivo) {
        return new IllegalStateException("Configuração de risco inválida (seção risco do application.yml): " + motivo);
    }
}
