package br.edu.exemplo.ia.repository.csv;

import br.edu.exemplo.ia.domain.entity.Admin;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.vo.AdminVO;
import br.edu.exemplo.ia.repository.AdminRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admins na base de dados em CSV (admins.csv, criado no primeiro cadastro).
 *
 * <p>Colunas: admin_id, company_id, nome, cpf, email, telefone, cargo, senha_hash, status_conta, nivel_acesso.
 * A senha fica só como hash BCrypt. Linhas de empresas que não existem em companies.csv são ignoradas.
 */
@Repository
public class AdminCsvRepository implements AdminRepository {

    private final TabelaCsv tabela;
    private final EmpresaCsvRepository empresas;
    private Snapshot snapshot;

    public AdminCsvRepository(BaseDadosCsv base, EmpresaCsvRepository empresas) {
        this.tabela = base.tabela(BaseDadosCsv.ADMINS);
        this.empresas = empresas;
    }

    @Override
    public synchronized List<Admin> findAll() {
        return atual().admins;
    }

    @Override
    public synchronized Optional<Admin> findById(UUID id) {
        return Optional.ofNullable(atual().porId.get(id));
    }

    public synchronized Optional<Admin> findByCodigo(String codigo) {
        return Optional.ofNullable(atual().porCodigo.get(codigo == null ? "" : codigo.trim()));
    }

    /** Muda sempre que admins.csv ou companies.csv é alterado. */
    public synchronized String versao() {
        return atual().versao;
    }

    @Override
    public synchronized Admin save(Admin admin) {
        String empresa = admin.getEmpresa().getCodigo();
        List<String> codigos = tabela.linhas().stream().map(l -> l.get("admin_id")).toList();
        admin.definirCodigo(Codigos.proximo(codigos, empresa + "-A", 3));
        Map<String, String> linha = new LinkedHashMap<>();
        linha.put("admin_id", admin.getCodigo());
        linha.put("company_id", empresa);
        linha.put("nome", admin.getNome());
        linha.put("cpf", Codigos.texto(admin.getCpf()));
        linha.put("email", admin.getEmail());
        linha.put("telefone", Codigos.texto(admin.getTelefone()));
        linha.put("cargo", admin.getCargo());
        linha.put("senha_hash", admin.getSenhaHash());
        linha.put("status_conta", admin.getStatusConta());
        linha.put("nivel_acesso", admin.getNivelAcesso());
        tabela.adicionar(linha);
        snapshot = null;
        return admin;
    }

    private Snapshot atual() {
        String versao = tabela.versao() + "|" + empresas.versao();
        if (snapshot == null || !snapshot.versao.equals(versao)) {
            List<Admin> admins = new ArrayList<>();
            for (Map<String, String> l : tabela.linhas()) {
                String codigo = Codigos.textoOuNulo(l.get("admin_id"));
                Optional<Empresa> empresa = empresas.findByCodigo(l.get("company_id"));
                if (codigo != null && empresa.isPresent()) {
                    admins.add(Admin.restaurar(codigo, new AdminVO(
                            Codigos.textoOuNulo(l.get("nome")) == null ? codigo : l.get("nome"),
                            Codigos.textoOuNulo(l.get("cpf")),
                            Codigos.textoOuNulo(l.get("email")),
                            Codigos.numeroOuNulo(l.get("telefone")),
                            Codigos.textoOuNulo(l.get("cargo")),
                            Codigos.textoOuNulo(l.get("senha_hash")),
                            Codigos.textoOuNulo(l.get("status_conta")),
                            Codigos.textoOuNulo(l.get("nivel_acesso"))
                    ), empresa.get()));
                }
            }
            snapshot = new Snapshot(versao, List.copyOf(admins),
                    admins.stream().collect(Collectors.toMap(Admin::getId, Function.identity(), (a, b) -> a)),
                    admins.stream().collect(Collectors.toMap(Admin::getCodigo, Function.identity(), (a, b) -> a)));
        }
        return snapshot;
    }

    private record Snapshot(String versao, List<Admin> admins, Map<UUID, Admin> porId, Map<String, Admin> porCodigo) {
    }
}
