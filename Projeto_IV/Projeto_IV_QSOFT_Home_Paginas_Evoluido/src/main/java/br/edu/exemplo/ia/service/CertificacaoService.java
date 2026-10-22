package br.edu.exemplo.ia.service;

import br.edu.exemplo.ia.domain.entity.Certificacao;
import br.edu.exemplo.ia.domain.entity.Funcionario;
import br.edu.exemplo.ia.domain.vo.TipoCertificacao;
import br.edu.exemplo.ia.dto.CertificacaoRequest;
import br.edu.exemplo.ia.dto.CertificacaoResponse;
import br.edu.exemplo.ia.dto.ResumoCertificacaoResponse;
import br.edu.exemplo.ia.repository.CertificacaoRepository;
import br.edu.exemplo.ia.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CertificacaoService implements CertificacaoUseCase {
    private final CertificacaoRepository certificacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PontuacaoAiLiteracy pontuacao;

    public CertificacaoService(CertificacaoRepository certificacaoRepository,
                               FuncionarioRepository funcionarioRepository,
                               PontuacaoAiLiteracy pontuacao) {
        this.certificacaoRepository = certificacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.pontuacao = pontuacao;
    }

    @Override
    public CertificacaoResponse registrar(CertificacaoRequest request) {
        Funcionario funcionario = loadFuncionario(request.funcionarioId());

        // Enquanto não existe login de funcionário, a empresa vem da tela e precisa ser a mesma do funcionário.
        if (request.empresaId() == null) {
            throw new IllegalArgumentException("Empresa obrigatória");
        }
        if (!funcionario.getEmpresa().getId().equals(request.empresaId())) {
            throw new IllegalArgumentException("Funcionário não pertence à empresa informada");
        }

        TipoCertificacao tipo = TipoCertificacao.converter(request.tipo());
        Certificacao certificacao = certificacaoRepository.save(new Certificacao(
                funcionario, request.nome(), request.instituicao(), request.dataConclusao(), tipo
        ));
        return toDto(certificacao);
    }

    @Override
    public List<CertificacaoResponse> listar(UUID funcionarioId) {
        loadFuncionario(funcionarioId);
        return certificacaoRepository.findByFuncionarioIdOrderByDataConclusaoDesc(funcionarioId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Total de capacitações e pontos de AI Literacy do funcionário.
     * Pensado para ser reaproveitado pelo AI Maturity Score e pelo cálculo de AI Literacy (PS-283).
     */
    @Override
    public ResumoCertificacaoResponse resumo(UUID funcionarioId) {
        loadFuncionario(funcionarioId);
        List<Certificacao> certificacoes = certificacaoRepository.findByFuncionarioIdOrderByDataConclusaoDesc(funcionarioId);

        int totalCursos = 0;
        int totalCertificacoes = 0;
        List<TipoCertificacao> tipos = new ArrayList<>();
        for (Certificacao certificacao : certificacoes) {
            if (certificacao.getTipo() == TipoCertificacao.CURSO) {
                totalCursos++;
            } else {
                totalCertificacoes++;
            }
            tipos.add(certificacao.getTipo());
        }

        return new ResumoCertificacaoResponse(
                funcionarioId,
                certificacoes.size(),
                totalCursos,
                totalCertificacoes,
                pontuacao.somar(tipos)
        );
    }

    private Funcionario loadFuncionario(UUID funcionarioId) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("Funcionário obrigatório");
        }
        return funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario inexistente: " + funcionarioId));
    }

    private CertificacaoResponse toDto(Certificacao certificacao) {
        return new CertificacaoResponse(
                certificacao.getId(),
                certificacao.getFuncionario().getId(),
                certificacao.getFuncionario().getNome(),
                certificacao.getNome(),
                certificacao.getInstituicao(),
                certificacao.getDataConclusao(),
                certificacao.getTipo().name(),
                pontuacao.pontosPor(certificacao.getTipo()),
                certificacao.getDataCriacao()
        );
    }
}
