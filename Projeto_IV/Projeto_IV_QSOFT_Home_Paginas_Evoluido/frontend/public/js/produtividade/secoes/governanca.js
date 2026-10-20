import { $, nf } from "../formato.js";
import { barrasHorizontais } from "../componentes/barras.js";
import { corTreinamento, rotuloTreinamento } from "../rotulos.js";

/** Eventos de segurança por tipo e treinamentos por situação; cada painel some se a tabela dele faltar. */
export default {
  secao: "gov",
  disponivel: X => X.gov != null && (X.gov.eventos_tipo != null || X.gov.trein_status != null),
  render(X, resumo) {
    const g = X.gov;
    $("#painel-seguranca").hidden = g.eventos_tipo == null;
    if (g.eventos_tipo) barrasHorizontais($("#hb-sec"), Object.entries(g.eventos_tipo).map(([k, v]) => ({ k, v })), v => nf(v), "var(--s2)");
    $("#painel-treinamento").hidden = g.trein_status == null;
    if (g.trein_status) {
      const concluidos = (resumo.valores || {})["treinamento-concluido"] || [];
      barrasHorizontais($("#hb-trn"), Object.entries(g.trein_status)
        .map(([s, v]) => ({ k: rotuloTreinamento(s), v, c: corTreinamento(s, concluidos) })), v => nf(v));
    }
  },
};
