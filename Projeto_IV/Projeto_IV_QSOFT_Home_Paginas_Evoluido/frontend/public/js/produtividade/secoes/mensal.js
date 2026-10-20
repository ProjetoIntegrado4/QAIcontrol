import { $, big, brl, brlFull, css, nf } from "../formato.js";
import { graficoLinhas } from "../componentes/graficoLinhas.js";

/** Evolução mensal: colaboradores ativos, requisições e custo x orçamento. */
export default {
  secao: "mensal",
  render(X) {
    const ms = X.mensal, labs = ms.map(m => m.mes);
    graficoLinhas($("#ch-ativos"), labs, [{ name: "Colaboradores", color: css("--s1"), values: ms.map(m => m.ativos), area: 1 }], v => nf(v), { aria: "Colaboradores ativos por mês" });
    graficoLinhas($("#ch-req"), labs, [{ name: "Requisições", color: css("--s1"), values: ms.map(m => m.req), area: 1 }], v => big(v), { aria: "Requisições por mês" });
    const temCusto = ms.some(m => m.custo != null || m.orcamento != null);
    $("#painel-custo").hidden = !temCusto;
    if (temCusto) {
      graficoLinhas($("#ch-custo"), labs, [
        { name: "Custo faturado", color: css("--s1"), values: ms.map(m => m.custo), area: 1 },
        { name: "Orçamento", color: css("--ref"), values: ms.map(m => m.orcamento || null), dash: 1 },
      ], (v, ax) => ax ? brl(v) : brlFull(v), { h: 230, aria: "Custo mensal comparado ao orçamento" });
    }
  },
};
