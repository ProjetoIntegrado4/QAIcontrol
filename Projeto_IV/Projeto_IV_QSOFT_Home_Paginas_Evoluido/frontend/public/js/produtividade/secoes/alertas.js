import { $, esc, nf, pct } from "../formato.js";
import { derivados, METAS } from "./indicadores.js";
import { selo } from "../componentes/visuais.js";

/**
 * Decisões recomendadas, geradas a partir dos dados da empresa. Cada regra devolve
 * [situação, o que os dados mostram, o que fazer] e só roda se os dados dela vieram.
 */
const REGRAS = [
  X => {
    const A = (X.areas || []).filter(a => a.funcionarios >= 5);
    if (A.length < 2) return null;
    const ord = [...A].sort((a, b) => a.adocao - b.adocao), lo = ord[0], hi = ord.at(-1);
    const abaixo = A.filter(a => a.adocao < METAS.adocao).length;
    return [lo.adocao < METAS.adocaoCritica ? "crit" : abaixo ? "warn" : "good",
      `<b>${esc(lo.area)}</b> tem a menor adoção (${pct(lo.adocao, 0)}), contra ${pct(hi.adocao, 0)} em ${esc(hi.area)}. ${abaixo} de ${A.length} áreas estão abaixo da meta de ${METAS.adocao}%.`,
      abaixo ? `Usar ${esc(hi.area)} como referência e levar casos de uso para ${esc(lo.area)}.` : "Todas as áreas na meta."];
  },
  (X, dias) => {
    const k = X.kpis, ocio = derivados(k).ociosidade;
    if (ocio == null || !k.lic_ociosas) return null;
    const pior = [...(X.ferramentas || [])].filter(f => f.lic_ociosas).sort((a, b) => b.lic_ociosas - a.lic_ociosas)[0];
    return [ocio > METAS.ociosidadeCritica ? "crit" : ocio > METAS.ociosidade ? "warn" : "good",
      `<b>${nf(k.lic_ociosas)} licenças</b> (${pct(ocio, 0)}) não tiveram uso nos últimos ${dias} dias${pior ? `, ${nf(pior.lic_ociosas)} delas de ${esc(pior.nome)}` : ""}.`,
      "Remanejar para quem está na fila ou cancelar na próxima renovação."];
  },
  X => X.kpis.fora_politica > METAS.fora
    ? [X.kpis.fora_politica > METAS.foraCritica ? "crit" : "warn",
      `<b>${pct(X.kpis.fora_politica)}</b> das requisições usaram ferramenta não aprovada para a empresa ou para a área do usuário.`,
      "Bloquear as ferramentas não aprovadas e comunicar a política de uso."]
    : null,
  X => {
    const k = X.kpis;
    return k.eventos ? [k.eventos_graves ? "crit" : "warn",
      `<b>${nf(k.eventos)} ${k.eventos === 1 ? "evento" : "eventos"} de segurança</b> com IA, ${nf(k.eventos_graves)} de severidade alta ou crítica.`,
      k.eventos_graves ? "Abrir investigação dos eventos graves com Segurança da Informação." : "Revisar os eventos com os gestores das áreas envolvidas."] : null;
  },
  X => {
    const k = X.kpis;
    return k.trein_concl != null ? [k.trein_concl < METAS.treinamentoCritico ? "crit" : k.trein_concl < METAS.treinamento ? "warn" : "good",
      `<b>${pct(k.trein_concl, 0)}</b> das matrículas em treinamentos de IA foram concluídas (${nf(k.trein_pessoas)} pessoas formadas).`,
      k.trein_concl < METAS.treinamento ? "Cobrar a conclusão das matrículas em aberto." : "Abrir trilhas avançadas para quem já concluiu."] : null;
  },
  X => {
    const ms = X.mensal || [], a3 = ms.slice(-3).reduce((s, m) => s + m.req, 0), b3 = ms.slice(-6, -3).reduce((s, m) => s + m.req, 0);
    if (!b3) return null;
    const g = 100 * (a3 / b3 - 1);
    return [g >= 0 ? "good" : g < -15 ? "crit" : "warn",
      `Requisições dos últimos 3 meses ${g >= 0 ? "cresceram" : "caíram"} <b>${pct(Math.abs(g), 0)}</b> frente aos 3 meses anteriores.`,
      g >= 0 ? "Uso em expansão: acompanhar se o custo cresce no mesmo ritmo." : "Entender a queda com as áreas antes de renovar licenças."];
  },
];

const PESO = { crit: 0, warn: 1, good: 2 };

export default {
  secao: "alertas",
  disponivel: X => X.kpis != null,
  render(X, resumo) {
    const itens = REGRAS.map(r => r(X, resumo.janela_dias)).filter(Boolean).sort((a, b) => PESO[a[0]] - PESO[b[0]]);
    $("#alerts").innerHTML = itens.map(([c, t, a]) =>
      `<li class="${c}">${selo(c)}<p>${t}</p><p class="acao"><b>Decisão:</b> ${a}</p></li>`).join("");
  },
};
