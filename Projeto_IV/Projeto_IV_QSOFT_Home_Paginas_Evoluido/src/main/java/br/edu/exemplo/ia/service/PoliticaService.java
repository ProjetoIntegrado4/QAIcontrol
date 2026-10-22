package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.AceitePolitica;
import br.edu.exemplo.ia.domain.entity.Empresa;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.entity.PoliticaUso;
import br.edu.exemplo.ia.dto.AceiteRequest;
import br.edu.exemplo.ia.dto.AceiteResponse;
import br.edu.exemplo.ia.dto.PoliticaRequest;
import br.edu.exemplo.ia.dto.PoliticaResponse;
import br.edu.exemplo.ia.repository.AceitePoliticaRepository;
import br.edu.exemplo.ia.repository.EmpresaRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import br.edu.exemplo.ia.repository.PoliticaUsoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Políticas de uso responsável de IA versionadas por empresa e aceite dos colaboradores (US PP-21).
 * Nenhuma versão ou aceite é apagado: uma alteração publica a versão seguinte e encerra a vigência da anterior.
 */
@Service
public class PoliticaService implements PoliticaUseCase {
    private final PoliticaUsoRepository politicaRepository;
    private final AceitePoliticaRepository aceiteRepository;
    private final EmpresaRepository empresaRepository;
    private final FuncionarioRepository funcionarioRepository;

    public PoliticaService(PoliticaUsoRepository politicaRepository,
                           AceitePoliticaRepository aceiteRepository,
                           EmpresaRepository empresaRepository,
                           FuncionarioRepository funcionarioRepository) {
        this.politicaRepository = politicaRepository;
        this.aceiteRepository = aceiteRepository;
        this.empresaRepository = empresaRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public PoliticaResponse publicar(PoliticaRequest request) {
        Empresa empresa = loadEmpresa(request.empresaId());
        PoliticaUso politica = politicaRepository.save(
                new PoliticaUso(empresa, request.titulo(), request.conteudo(), request.obrigatoria())
        );
        return toDto(politica);
    }

    @Override
    @Transactional
    public PoliticaResponse novaVersao(UUID codigo, PoliticaRequest request) {
        PoliticaUso atual = loadVersoes(codigo).get(0);
        if (request.empresaId() != null && !request.empresaId().equals(atual.getEmpresa().getId())) {
            throw new IllegalArgumentException("A empresa de uma política não pode ser alterada");
        }

        PoliticaUso proxima = atual.novaVersao(request.titulo(), request.conteudo(), request.obrigatoria());
        politicaRepository.save(atual);
        return toDto(politicaRepository.save(proxima));
    }

    @Override
    public List<PoliticaResponse> vigentes(UUID empresaId) {
        loadEmpresa(empresaId);
        return politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<PoliticaResponse> versoes(UUID codigo) {
        return loadVersoes(codigo).stream()
                .map(this::toDto)
                .toList();
    }

    /** Versões vigentes e obrigatórias da empresa que o colaborador ainda não aceitou (CA-15.1, BDD 2). */
    @Override
    public List<PoliticaResponse> pendentes(UUID empresaId, UUID funcionarioId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa obrigatória");
        }
        Funcionario funcionario = loadFuncionario(funcionarioId);
        // Enquanto não existe login de funcionário, a empresa vem da tela e precisa ser a mesma do funcionário.
        if (!funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Funcionário não pertence à empresa informada");
        }

        Set<UUID> aceitas = aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionarioId).stream()
                .map(aceite -> aceite.getPolitica().getId())
                .collect(Collectors.toSet());
        return politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresaId).stream()
                .filter(PoliticaUso::isObrigatoria)
                .filter(politica -> !aceitas.contains(politica.getId()))
                .map(this::toDto)
                .toList();
    }

    /**
     * Registra o aceite da versão informada (CA-15.2/CA-15.3).
     * Aceitar de novo a mesma versão devolve o aceite já existente, sem duplicar nem mudar a data.
     */
    @Override
    public AceiteResponse aceitar(AceiteRequest request) {
        Funcionario funcionario = loadFuncionario(request.funcionarioId());
        if (request.politicaId() == null) {
            throw new IllegalArgumentException("Política obrigatória");
        }
        PoliticaUso politica = politicaRepository.findById(request.politicaId())
                .orElseThrow(() -> new NaoEncontradoException("Política inexistente: " + request.politicaId()));

        return aceiteRepository.findByFuncionarioIdAndPoliticaId(funcionario.getId(), politica.getId())
                .map(this::toDto)
                .orElseGet(() -> toDto(registrarAceite(funcionario, politica)));
    }

    @Override
    public List<AceiteResponse> aceites(UUID funcionarioId) {
        loadFuncionario(funcionarioId);
        return aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionarioId).stream()
                .map(this::toDto)
                .toList();
    }

    private AceitePolitica registrarAceite(Funcionario funcionario, PoliticaUso politica) {
        AceitePolitica aceite = new AceitePolitica(funcionario, politica);
        try {
            return aceiteRepository.save(aceite);
        } catch (DataIntegrityViolationException e) {
            // Dois cliques simultâneos: a restrição única do banco barrou o segundo; devolve o que foi gravado.
            return aceiteRepository.findByFuncionarioIdAndPoliticaId(funcionario.getId(), politica.getId())
                    .orElseThrow(() -> e);
        }
    }

    private List<PoliticaUso> loadVersoes(UUID codigo) {
        List<PoliticaUso> versoes = politicaRepository.findByCodigoOrderByVersaoDesc(codigo);
        if (versoes.isEmpty()) {
            throw new NaoEncontradoException("Política inexistente: " + codigo);
        }
        return versoes;
    }

    private Empresa loadEmpresa(UUID empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa obrigatória");
        }
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new NaoEncontradoException("Empresa inexistente: " + empresaId));
    }

    private Funcionario loadFuncionario(UUID funcionarioId) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("Funcionário obrigatório");
        }
        return funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new NaoEncontradoException("Funcionário inexistente: " + funcionarioId));
    }

    private PoliticaResponse toDto(PoliticaUso politica) {
        return new PoliticaResponse(
                politica.getId(),
                politica.getCodigo(),
                politica.getEmpresa().getId(),
                politica.getEmpresa().getNome(),
                politica.getTitulo(),
                politica.getConteudo(),
                politica.getVersao(),
                politica.isObrigatoria(),
                politica.isVigente(),
                politica.getPublicadaEm()
        );
    }

    private AceiteResponse toDto(AceitePolitica aceite) {
        return new AceiteResponse(
                aceite.getId(),
                aceite.getFuncionario().getId(),
                aceite.getFuncionario().getNome(),
                aceite.getPolitica().getId(),
                aceite.getPolitica().getCodigo(),
                aceite.getPolitica().getTitulo(),
                aceite.getVersao(),
                aceite.getDataAceite()
        );
    }
}
