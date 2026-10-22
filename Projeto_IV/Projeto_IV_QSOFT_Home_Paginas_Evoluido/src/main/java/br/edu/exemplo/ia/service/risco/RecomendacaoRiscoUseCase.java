package br.edu.exemplo.ia.service.risco;

import br.edu.exemplo.ia.dto.RecomendacaoRiscoResponse;
import br.edu.exemplo.ia.dto.RegrasRiscoResponse;
import br.edu.exemplo.ia.dto.UnidadeRiscoRequest;
import br.edu.exemplo.ia.dto.UnidadeRiscoResponse;

import java.util.List;

public interface RecomendacaoRiscoUseCase {
    /** Versão, vigência, critério e faixas com limites e recomendações (memória da regra, RN17). */
    RegrasRiscoResponse regras();

    /** Faixa e recomendações de um score de 0 a 100; sem score, "dados insuficientes" e nenhuma recomendação. */
    RecomendacaoRiscoResponse recomendar(String score);

    /** Faixa e recomendações de cada unidade (colaborador, área...) num período, na mesma ordem recebida. */
    List<UnidadeRiscoResponse> recomendarLote(List<UnidadeRiscoRequest> unidades);
}
