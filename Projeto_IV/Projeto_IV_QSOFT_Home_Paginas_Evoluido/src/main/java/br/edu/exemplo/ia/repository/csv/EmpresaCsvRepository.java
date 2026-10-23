package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.EmpresaDadosVO;
import br.edu.exemplo.ia.domain.vo.EmpresaVO;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Empresas na base de dados em CSV (companies.csv).
 *
 * <p>Colunas da base: company_id, company_name, industry, company_size, employee_count.
 * Os dados que só o site tem (razão social, CNPJ, endereço...) ficam em colunas extras no fim do arquivo.
 */
@Repository
public class EmpresaCsvRepository implements EmpresaRepository {

    private final TabelaCsv tabela;
    private Snapshot snapshot;

    public EmpresaCsvRepository(BaseDadosCsv base) {
        this.tabela = base.tabela(BaseDadosCsv.EMPRESAS);
    }

    @Override
    public synchronized List<Empresa> findAll() {
        return atual().empresas;
    }

    @Override
    public synchronized Optional<Empresa> findById(UUID id) {
        return Optional.ofNullable(atual().porId.get(id));
    }

    public synchronized Optional<Empresa> findByCodigo(String codigo) {
        return Optional.ofNullable(atual().porCodigo.get(codigo == null ? "" : codigo.trim()));
    }

    /** Muda sempre que companies.csv é alterado. */
    public synchronized String versao() {
        return atual().versao;
    }

    @Override
    public synchronized Empresa save(Empresa empresa) {
        empresa.definirCodigo(Codigos.proximo(atual().porCodigo.keySet(), "C", 2));
        Map<String, String> linha = new LinkedHashMap<>();
        linha.put("company_id", empresa.getCodigo());
        linha.put("company_name", empresa.getNome());
        linha.put("industry", empresa.getArea());
        linha.put("company_size", porte(empresa.getNumeroFuncionarios()));
        linha.put("employee_count", Codigos.texto(empresa.getNumeroFuncionarios()));
        linha.put("razao_social", Codigos.texto(empresa.getRazaoSocial()));
        linha.put("cnpj", Codigos.texto(empresa.getCnpj()));
        linha.put("cep", Codigos.texto(empresa.getCep()));
        linha.put("endereco", Codigos.texto(empresa.getEndereco()));
        linha.put("cidade", Codigos.texto(empresa.getCidade()));
        linha.put("estado", Codigos.texto(empresa.getEstado()));
        linha.put("admin_responsavel", Codigos.texto(empresa.getAdminResponsavel()));
        tabela.adicionar(linha);
        snapshot = null;
        return empresa;
    }

    /** Porte no mesmo padrão da base: Pequena, Media ou Grande. */
    static String porte(int funcionarios) {
        if (funcionarios < 200) {
            return "Pequena";
        }
        return funcionarios < 1000 ? "Media" : "Grande";
    }

    private Snapshot atual() {
        String versao = tabela.versao();
        if (snapshot == null || !snapshot.versao.equals(versao)) {
            List<Empresa> empresas = tabela.linhas().stream()
                    .filter(l -> Codigos.textoOuNulo(l.get("company_id")) != null)
                    .map(EmpresaCsvRepository::empresa)
                    .toList();
            snapshot = new Snapshot(versao, empresas,
                    empresas.stream().collect(Collectors.toMap(Empresa::getId, Function.identity(), (a, b) -> a)),
                    empresas.stream().collect(Collectors.toMap(Empresa::getCodigo, Function.identity(), (a, b) -> a)));
        }
        return snapshot;
    }

    private static Empresa empresa(Map<String, String> l) {
        String codigo = l.get("company_id").trim();
        String nome = Codigos.textoOuNulo(l.get("company_name"));
        Long funcionarios = Codigos.numeroOuNulo(l.get("employee_count"));
        return Empresa.restaurar(codigo, new EmpresaVO(nome == null ? codigo : nome), new EmpresaDadosVO(
                Codigos.textoOuNulo(l.get("industry")),
                Codigos.textoOuNulo(l.get("razao_social")),
                Codigos.textoOuNulo(l.get("cnpj")),
                funcionarios == null ? null : funcionarios.intValue(),
                Codigos.textoOuNulo(l.get("cep")),
                Codigos.textoOuNulo(l.get("endereco")),
                Codigos.textoOuNulo(l.get("cidade")),
                Codigos.textoOuNulo(l.get("estado")),
                Codigos.textoOuNulo(l.get("admin_responsavel"))
        ));
    }

    private record Snapshot(String versao, List<Empresa> empresas, Map<UUID, Empresa> porId,
                            Map<String, Empresa> porCodigo) {
    }
}
