import { $, esc, nf, pct } from "../formato.js";
import { derivados } from "./indicadores.js";

/** Pontos de atenção gerados a partir dos dados da empresa. Cada regra só roda se os dados dela vieram. */
const REGRAS = [
  X => {
    const A = (X.areas || []).filter(a => a.funcionarios >= 5);
    if (!A.length) return null;
    const lo = [...A].sort((a, b) => a.adocao - b.adocao)[0], hi = [...A].sort((a, b) => b.adocao - a.adocao)[0];
    return ["warn", `<b>${esc(lo.area)}</b> tem a menor adoção (${pct(lo.adocao, 0)}), contra ${pct(hi.adocao, 0)} em ${esc(hi.area)}.`];
  },
  (X, dias) => {
    const k = X.kpis, ocio = derivados(k).ociosidade;
    if (ocio == null || !k.lic_ociosas) return null;
    return [ocio > 35 ? "crit" : "warn", `<b>${nf(k.lic_ociosas)} licenças</b> ativas não tiveram uso nos últimos ${dias} dias. Candidatas a remanejamento ou cancelamento.`];
  },
  X => X.kpis.fora_politica > 0.5
    ? [X.kpis.fora_politica > 5 ? "crit" : "warn", `<b>${pct(X.kpis.fora_politica)}</b> das requisições usaram ferramenta não aprovada para a empresa ou para a área do usuário.`]
    : null,
  X => {
    const k = X.kpis;
    return k.eventos ? [k.eventos_graves ? "crit" : "warn", `<b>${nf(k.eventos)} ${k.eventos === 1 ? "evento" : "eventos"} de segurança</b> com IA, ${nf(k.eventos_graves)} de severidade alta ou crítica.`] : null;
  },
  X => {
    const k = X.kpis;
    return k.trein_concl != null ? [k.trein_concl < 60 ? "warn" : "good", `<b>${pct(k.trein_concl, 0)}</b> das matrículas em treinamentos de IA foram concluídas (${nf(k.trein_pessoas)} pessoas formadas).`] : null;
  },
  X => {
    const ms = X.mensal || [], a3 = ms.slice(-3).reduce((s, m) => s + m.req, 0), b3 = ms.slice(-6, -3).reduce((s, m) => s + m.req, 0);
    if (!b3) return null;
    const g = 100 * (a3 / b3 - 1);
    return [g >= 0 ? "good" : "warn", `Requisições dos últimos 3 meses ${g >= 0 ? "cresceram" : "caíram"} <b>${pct(Math.abs(g), 0)}</b> frente aos 3 meses anteriores.`];
  },
];

export default {
  secao: "alertas",
  disponivel: X => X.kpis != null,
  render(X, resumo) {
    const itens = REGRAS.map(r => r(X, resumo.janela_dias)).filter(Boolean);
    $("#alerts").innerHTML = itens.map(([c, t]) => `<li class="${c}">${t}</li>`).join("");
  },
};
