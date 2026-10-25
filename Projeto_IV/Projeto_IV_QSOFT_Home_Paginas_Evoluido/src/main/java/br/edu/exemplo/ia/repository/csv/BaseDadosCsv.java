package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.config.BaseDadosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Pasta da base de dados em CSV (qsoft.dados.dir) e os arquivos usados pelos cadastros. */
@Component
public class BaseDadosCsv {
    public static final String EMPRESAS = "companies.csv";
    public static final String FUNCIONARIOS = "employees.csv";
    public static final String DEPARTAMENTOS = "departments.csv";
    public static final String ADMINS = "admins.csv";
    public static final String TREINAMENTOS = "training_records.csv";

    private static final Logger log = LoggerFactory.getLogger(BaseDadosCsv.class);

    private final Path pasta;
    private final Map<String, TabelaCsv> tabelas = new ConcurrentHashMap<>();

    public BaseDadosCsv(BaseDadosProperties props) {
        this.pasta = Path.of(props.dir());
        log.info("Base de dados dos cadastros: {}", pasta.toAbsolutePath());
    }

    public Path pasta() {
        return pasta;
    }

    /** O arquivo da pasta com esse nome. Sempre a mesma instância, para que as gravações não se cruzem. */
    public TabelaCsv tabela(String arquivo) {
        return tabelas.computeIfAbsent(arquivo, nome -> new TabelaCsv(pasta.resolve(nome)));
    }
}
