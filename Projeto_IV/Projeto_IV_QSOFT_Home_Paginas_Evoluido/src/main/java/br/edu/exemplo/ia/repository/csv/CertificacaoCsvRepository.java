package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.domain.entity.Certificacao;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import br.edu.exemplo.ia.repository.CertificacaoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cursos e certificações na base de dados em CSV (training_records.csv), o mesmo arquivo dos treinamentos
 * que o painel de produtividade já usa.
 *
 * <p>Colunas da base: training_id, employee_id, company_id, course_name, course_category, enrollment_date,
 * completion_date, completion_status, score, duration_hours. Só entram as linhas concluídas
 * (completion_status "Concluido") com data de conclusão. O que só o site tem (instituição, tipo e data do
 * registro) fica em colunas extras no fim do arquivo; nas linhas antigas, sem essas colunas, a instituição
 * aparece como "Não informada" e o tipo como CURSO.
 */
@Repository
public class CertificacaoCsvRepository implements CertificacaoRepository {

    static final String STATUS_CONCLUIDO = "Concluido";
    static final String SEM_INSTITUICAO = "Não informada";

    private final TabelaCsv tabela;
    private Snapshot snapshot;

    public CertificacaoCsvRepository(BaseDadosCsv base) {
        this.tabela = base.tabela(BaseDadosCsv.TREINAMENTOS);
    }

    @Override
    public synchronized List<Certificacao> findByFuncionarioIdOrderByDataConclusaoDesc(UUID funcionarioId) {
        return atual().porFuncionario.getOrDefault(funcionarioId, List.of());
    }

    @Override
    public synchronized Certificacao save(Certificacao certificacao) {
        Funcionario funcionario = certificacao.getFuncionario();
        if (funcionario == null || funcionario.getCodigo() == null) {
            throw new IllegalArgumentException("Funcionário da certificação não está na base de dados");
        }
        List<String> codigos = tabela.linhas().stream().map(l -> l.get("training_id")).toList();
        certificacao.definirCodigo(Codigos.proximo(codigos, "TR", 6));
        String conclusao = certificacao.getDataConclusao().toString();

        Map<String, String> linha = new LinkedHashMap<>();
        linha.put("training_id", certificacao.getCodigo());
        linha.put("employee_id", funcionario.getCodigo());
        linha.put("company_id", Codigos.texto(funcionario.getEmpresa().getCodigo()));
        linha.put("course_name", certificacao.getNome());
        linha.put("course_category", "");
        linha.put("enrollment_date", conclusao);
        linha.put("completion_date", conclusao);
        linha.put("completion_status", STATUS_CONCLUIDO);
        linha.put("score", "");
        linha.put("duration_hours", "");
        linha.put("instituicao", certificacao.getInstituicao());
        linha.put("tipo", certificacao.getTipo().name());
        linha.put("registrado_em", certificacao.getDataCriacao().toString());
        tabela.adicionar(linha);
        snapshot = null;
        return certificacao;
    }

    private Snapshot atual() {
        String versao = tabela.versao();
        if (snapshot == null || !snapshot.versao.equals(versao)) {
            List<Certificacao> todas = new ArrayList<>();
            for (Map<String, String> l : tabela.linhas()) {
                Certificacao c = certificacao(l);
                if (c != null) {
                    todas.add(c);
                }
            }
            Map<UUID, List<Certificacao>> porFuncionario = todas.stream()
                    .sorted(Comparator.comparing(Certificacao::getDataConclusao).reversed()
                            .thenComparing(Certificacao::getCodigo))
                    .collect(Collectors.groupingBy(Certificacao::getFuncionarioId, LinkedHashMap::new,
                            Collectors.toUnmodifiableList()));
            snapshot = new Snapshot(versao, porFuncionario);
        }
        return snapshot;
    }

    /** Linha da base como certificação; nula se não estiver concluída ou faltar código, funcionário ou data. */
    private static Certificacao certificacao(Map<String, String> l) {
        String codigo = Codigos.textoOuNulo(l.get("training_id"));
        String funcionario = Codigos.textoOuNulo(l.get("employee_id"));
        LocalDate conclusao = data(l.get("completion_date"));
        if (codigo == null || funcionario == null || conclusao == null || !concluido(l.get("completion_status"))) {
            return null;
        }
        String nome = Codigos.textoOuNulo(l.get("course_name"));
        String instituicao = Codigos.textoOuNulo(l.get("instituicao"));
        return Certificacao.restaurar(codigo, funcionario,
                nome == null ? codigo : nome,
                instituicao == null ? SEM_INSTITUICAO : instituicao,
                conclusao,
                tipo(l.get("tipo")),
                registradoEm(l.get("registrado_em"), conclusao));
    }

    static boolean concluido(String status) {
        return Codigos.chave(status).equals(Codigos.chave(STATUS_CONCLUIDO));
    }

    private static TipoCertificacao tipo(String valor) {
        try {
            return TipoCertificacao.converter(valor);
        } catch (IllegalArgumentException e) {
            return TipoCertificacao.CURSO;
        }
    }

    private static LocalDate data(String valor) {
        String texto = Codigos.textoOuNulo(valor);
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto.length() > 10 ? texto.substring(0, 10) : texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Instant registradoEm(String valor, LocalDate conclusao) {
        String texto = Codigos.textoOuNulo(valor);
        if (texto != null) {
            try {
                return Instant.parse(texto);
            } catch (DateTimeParseException e) {
                // Valor editado à mão: usa a data de conclusão.
            }
        }
        return conclusao.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private record Snapshot(String versao, Map<UUID, List<Certificacao>> porFuncionario) {
    }
}
