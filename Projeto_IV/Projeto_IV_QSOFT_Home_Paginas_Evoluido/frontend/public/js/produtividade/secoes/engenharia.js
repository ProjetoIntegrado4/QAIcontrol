import { $, css, esc, nf, pct } from "../formato.js";
import { graficoLinhas } from "../componentes/graficoLinhas.js";
import { tabela } from "../componentes/tabela.js";
import { cartao } from "./indicadores.js";

/** Entrega de software e relação entre uso de IA e pull requests. Só aparece para empresas com pull requests. */
export default {
  secao: "eng",
  tabelas: ["#t-coorte"],
  render(X) {
    const E = X.eng, t = E.totais;
    $("#eng-escopo").textContent = E.empresas_com_projetos.length
      ? `Disponível para as empresas com projetos de software (${E.empresas_com_projetos.map(esc).join(", ")})` : "";
    $("#eng-kpis").innerHTML = [
      ["Pull requests", nf(t.prs), `${nf(t.merged)} mergeados · ${nf(t.devs)} desenvolvedores`],
      ["Tempo até o merge", nf(t.lead) + " h", "mediana, PRs mergeados"],
      ["Builds com sucesso", pct(t.build_ok), "todas as execuções"],
      ["Deploys com falha ou revertidos", pct(t.deploy_falha), "todos os ambientes"],
    ].map(cartao).join("");
    const em = E.mensal;
    graficoLinhas($("#ch-pr"), em.map(m => m.mes), [{ name: "PRs mergeados", color: css("--s1"), values: em.map(m => m.merged), area: 1 }], v => nf(v), { aria: "PRs mergeados por mês" });
    const temBuild = em.some(m => m.build_ok != null);
    $("#ch-build").parentElement.hidden = !temBuild;
    if (temBuild) {
      graficoLinhas($("#ch-build"), em.map(m => m.mes), [{ name: "Builds com sucesso", color: css("--s3"), values: em.map(m => m.build_ok) }], (v, ax) => pct(v, ax ? 0 : 1), { fit: 1, aria: "Taxa de sucesso de builds por mês" });
    }
    tabela($("#t-coorte"), [
      { k: "faixa", t: "Faixa de uso de IA" }, { k: "devs", t: "Desenvolvedores", n: 1, f: v => nf(v) },
      { k: "ai_mediana", t: "Requisições de IA (mediana)", n: 1, f: v => nf(v) }, { k: "prs", t: "PRs por dev", n: 1, f: v => nf(v, 1) },
      { k: "merged", t: "Mergeados por dev", n: 1, f: v => nf(v, 1) }, { k: "lead", t: "Horas até merge (mediana)", n: 1, f: v => nf(v) },
    ], E.coortes, "ai_mediana", 1);
    const c = E.corr;
    $("#eng-note").innerHTML = c == null
      ? "Não há desenvolvedores suficientes para calcular a correlação entre uso de IA e PRs mergeados."
      : Math.abs(c) >= 0.3
        ? `A correlação entre uso de IA e PRs mergeados é <b>${nf(c, 2)}</b>: há relação ${c > 0 ? "positiva" : "negativa"} relevante entre uso de IA e volume de entrega.`
        : `A correlação entre uso de IA e PRs mergeados por desenvolvedor é <b>${nf(c, 2)}</b>, praticamente nula. Nesta base, quem usa mais IA não entrega mais PRs nem mais rápido. Vale medir outros resultados (qualidade, retrabalho, escopo) antes de atribuir ganhos de entrega à IA.`;
  },
};
