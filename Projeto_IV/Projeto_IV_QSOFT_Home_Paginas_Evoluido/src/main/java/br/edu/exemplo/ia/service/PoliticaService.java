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
import java.util.Optional;
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

    // Transação: encerrar a vigência da versão atual e gravar a nova acontecem juntas ou não acontecem.
    @Override
    @Transactional
    public PoliticaResponse publicarNovaVersao(UUID codigo, PoliticaRequest request) {
        PoliticaUso versaoAtual = loadVersaoVigente(codigo);
        validarMesmaEmpresa(versaoAtual, request.empresaId());

        PoliticaUso novaVersao = versaoAtual.novaVersao(request.titulo(), request.conteudo(), request.obrigatoria());
        politicaRepository.save(versaoAtual);
        return toDto(politicaRepository.save(novaVersao));
    }

    @Override
    public List<PoliticaResponse> listarVigentes(UUID empresaId) {
        loadEmpresa(empresaId);
        return politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<PoliticaResponse> listarVersoes(UUID codigo) {
        return loadVersoes(codigo).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<PoliticaResponse> listarPendentes(UUID empresaId, UUID funcionarioId) {
        loadFuncionarioDaEmpresa(empresaId, funcionarioId);
        Set<UUID> versoesAceitas = idsDasVersoesAceitas(funcionarioId);
        return politicaRepository.findByEmpresaIdAndVigenteTrueOrderByTituloAsc(empresaId).stream()
                .filter(PoliticaUso::isObrigatoria)
                .filter(politica -> !versoesAceitas.contains(politica.getId()))
                .map(this::toDto)
                .toList();
    }

    @Override
    public AceiteResponse registrarAceite(AceiteRequest request) {
        Funcionario funcionario = loadFuncionario(request.funcionarioId());
        PoliticaUso politica = loadPolitica(request.politicaId());

        // Aceite repetido da mesma versão devolve o que já existe, sem duplicar nem mudar a data (RN15).
        Optional<AceitePolitica> aceiteExistente =
                aceiteRepository.findByFuncionarioIdAndPoliticaId(funcionario.getId(), politica.getId());
        if (aceiteExistente.isPresent()) {
            return toDto(aceiteExistente.get());
        }
        return toDto(salvarAceite(new AceitePolitica(funcionario, politica)));
    }

    @Override
    public List<AceiteResponse> listarAceites(UUID funcionarioId) {
        loadFuncionario(funcionarioId);
        return aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionarioId).stream()
                .map(this::toDto)
                .toList();
    }

    private AceitePolitica salvarAceite(AceitePolitica aceite) {
        try {
            return aceiteRepository.save(aceite);
        } catch (DataIntegrityViolationException e) {
            // Dois cliques ao mesmo tempo: a restrição única do banco barrou o segundo; devolve o que foi gravado.
            return aceiteRepository.findByFuncionarioIdAndPoliticaId(
                    aceite.getFuncionario().getId(), aceite.getPolitica().getId()
            ).orElseThrow(() -> e);
        }
    }

    private Set<UUID> idsDasVersoesAceitas(UUID funcionarioId) {
        return aceiteRepository.findByFuncionarioIdOrderByDataAceiteDesc(funcionarioId).stream()
                .map(aceite -> aceite.getPolitica().getId())
                .collect(Collectors.toSet());
    }

    private void validarMesmaEmpresa(PoliticaUso politica, UUID empresaId) {
        // Na nova versão a empresa pode ser omitida, mas não trocada.
        if (empresaId != null && !empresaId.equals(politica.getEmpresa().getId())) {
            throw new IllegalArgumentException("A empresa de uma política não pode ser alterada");
        }
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

    private Funcionario loadFuncionarioDaEmpresa(UUID empresaId, UUID funcionarioId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa obrigatória");
        }
        Funcionario funcionario = loadFuncionario(funcionarioId);
        // Enquanto não existe login de funcionário, a empresa vem da tela e precisa ser a mesma do funcionário.
        if (!funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Funcionário não pertence à empresa informada");
        }
        return funcionario;
    }

    private PoliticaUso loadPolitica(UUID politicaId) {
        if (politicaId == null) {
            throw new IllegalArgumentException("Política obrigatória");
        }
        return politicaRepository.findById(politicaId)
                .orElseThrow(() -> new NaoEncontradoException("Política inexistente: " + politicaId));
    }

    private List<PoliticaUso> loadVersoes(UUID codigo) {
        List<PoliticaUso> versoes = politicaRepository.findByCodigoOrderByVersaoDesc(codigo);
        if (versoes.isEmpty()) {
            throw new NaoEncontradoException("Política inexistente: " + codigo);
        }
        return versoes;
    }

    // As versões vêm da mais recente para a mais antiga, e só a mais recente é vigente.
    private PoliticaUso loadVersaoVigente(UUID codigo) {
        return loadVersoes(codigo).get(0);
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
