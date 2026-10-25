import { $, big, brl, esc, nf, pct } from "../formato.js";
import { horas, preferencias } from "../preferencias.js";
import { regua, selo, SITUACAO } from "../componentes/visuais.js";

/** Classifica um valor: "good", "warn" ou "crit" (inv = quanto menor, melhor). */
export const tom = (v, good, bad, inv) => v == null ? "" : (inv ? v <= good : v >= good) ? "good" : (inv ? v >= bad : v <= bad) ? "crit" : "warn";

/** Metas usadas no diagnóstico, nos pontos de atenção e nos gráficos por área. */
export const METAS = { adocao: 70, adocaoCritica: 40, roi: 2, orcamento: 90, ociosidade: 15, ociosidadeCritica: 35, fora: 0.5, foraCritica: 5, treinamento: 70, treinamentoCritico: 50 };

/** Indicadores derivados usados pelos cartões e pelos pontos de atenção. */
export function derivados(k) {
  const h = horas(k.requisicoes), valor = h * preferencias.hora;
  return {
    horas: h,
    valor,
    roi: k.custo ? valor / k.custo : null,
    usoOrcamento: k.orcamento ? 100 * k.custo / k.orcamento : null,
    ociosidade: k.lic_ativas ? 100 * k.lic_ociosas / k.lic_ativas : null,
  };
}

const cartao = ([titulo, valor, detalhe, classe, pill]) =>
  `<div class="kpi"><span class="eyebrow">${titulo}</span><span class="v">${valor}</span><span class="d">${detalhe}</span>${classe ? `<span class="pill ${classe}">${pill}</span>` : ""}</div>`;

/** Área com menor adoção (entre as que têm ao menos 5 pessoas), para sugerir onde agir. */
export function areaMenosAdotada(X) {
  const A = (X.areas || []).filter(a => a.funcionarios >= 5);
  return A.length ? [...A].sort((a, b) => a.adocao - b.adocao)[0] : null;
}

/**
 * Seis perguntas que um gestor faz sobre IA na empresa, cada uma com resposta em semáforo,
 * uma régua com a meta e o que fazer. Indicador sem dados no banco não aparece.
 */
function diagnostico(X, resumo) {
  const k = X.kpis, d = derivados(k), M = METAS, baixa = areaMenosAdotada(X);
  const conf = k.fora_politica == null && k.eventos == null ? null
    : (k.fora_politica || 0) > M.foraCritica || k.eventos_graves > 0 ? "crit"
      : (k.fora_politica || 0) > M.fora || k.eventos > 0 ? "warn" : "good";
  return [
    k.adocao != null && {
      q: "As pessoas estão usando IA?", t: tom(k.adocao, M.adocao, M.adocaoCritica), v: pct(k.adocao, 0),
      d: `${nf(k.usuarios)} de ${nf(k.funcionarios)} colaboradores ativos`,
      r: regua(k.adocao, 100, M.adocao, tom(k.adocao, M.adocao, M.adocaoCritica), `meta ${M.adocao}%`), meta: `meta ${M.adocao}%`,
      a: k.adocao >= M.adocao ? "Manter: registrar as boas práticas das áreas que mais usam."
        : `Levar treinamento e casos de uso para ${baixa ? `<b>${esc(baixa.area)}</b> (${pct(baixa.adocao, 0)})` : "as áreas com menor adoção"}.`,
    },
    d.roi != null && {
      q: "O investimento se paga?", t: tom(d.roi, M.roi, 1), v: nf(d.roi, 1) + "×",
      d: `${brl(d.valor)} em tempo poupado (est.) para ${brl(k.custo)} de custo`,
      r: regua(d.roi, Math.max(4, d.roi * 1.1), M.roi, tom(d.roi, M.roi, 1), `meta ${M.roi}×`), meta: `meta ${M.roi}× o custo`,
      a: d.roi >= M.roi ? "Pode ampliar: cada real gasto volta multiplicado."
        : d.roi < 1 ? "Custa mais do que devolve: corte licenças ociosas antes de ampliar." : "Retorno marginal: priorize as ferramentas mais baratas por requisição.",
    },
    d.usoOrcamento != null && {
      q: "Estamos dentro do orçamento?", t: tom(d.usoOrcamento, M.orcamento, 100, true), v: pct(d.usoOrcamento, 0),
      d: `${brl(k.custo)} gastos de ${brl(k.orcamento)} orçados`,
      r: regua(d.usoOrcamento, Math.max(120, d.usoOrcamento * 1.05), 100, tom(d.usoOrcamento, M.orcamento, 100, true), "limite 100% do orçamento"), meta: "limite: 100% do orçado",
      a: d.usoOrcamento > 100 ? "Estourou: congele novas licenças e revise o contrato mais caro."
        : d.usoOrcamento > M.orcamento ? "No limite: acompanhe o gasto mês a mês." : `Há folga de ${brl(k.orcamento - k.custo)} no orçamento.`,
    },
    d.ociosidade != null && {
      q: "Estamos pagando por licença sem uso?", t: tom(d.ociosidade, M.ociosidade, M.ociosidadeCritica, true), v: nf(k.lic_ociosas),
      d: `${pct(d.ociosidade, 0)} das ${nf(k.lic_ativas)} licenças sem uso em ${resumo.janela_dias} dias`,
      r: regua(d.ociosidade, 100, M.ociosidade, tom(d.ociosidade, M.ociosidade, M.ociosidadeCritica, true), `aceitável até ${M.ociosidade}%`), meta: `aceitável até ${M.ociosidade}%`,
      a: d.ociosidade <= M.ociosidade ? "Licenças bem aproveitadas." : `Remanejar ou cancelar ${nf(k.lic_ociosas)} licenças paradas.`,
    },
    conf && {
      q: "O uso é seguro e dentro da política?", t: conf, v: pct(k.fora_politica || 0),
      d: `das requisições fora da política · ${nf(k.eventos || 0)} eventos de segurança (${nf(k.eventos_graves || 0)} graves)`,
      r: regua(k.fora_politica || 0, Math.max(10, (k.fora_politica || 0) * 1.2), M.fora, conf, `aceitável até ${nf(M.fora, 1)}%`), meta: `aceitável até ${nf(M.fora, 1)}%`,
      a: conf === "good" ? "Sem desvios relevantes."
        : k.eventos_graves > 0 ? "Investigar os eventos graves e reforçar a política com as equipes."
          : "Bloquear as ferramentas não aprovadas e comunicar a política de uso.",
    },
    k.trein_concl != null && {
      q: "As pessoas estão preparadas?", t: tom(k.trein_concl, M.treinamento, M.treinamentoCritico), v: pct(k.trein_concl, 0),
      d: `dos treinamentos de IA concluídos · ${nf(k.trein_pessoas)} pessoas formadas`,
      r: regua(k.trein_concl, 100, M.treinamento, tom(k.trein_concl, M.treinamento, M.treinamentoCritico), `meta ${M.treinamento}%`), meta: `meta ${M.treinamento}%`,
      a: k.trein_concl >= M.treinamento ? "Boa formação: abrir trilhas avançadas." : "Cobrar a conclusão das matrículas em aberto com os gestores.",
    },
  ].filter(Boolean);
}

function resumoGeral(itens) {
  const n = t => itens.filter(i => i.t === t).length, ok = n("good"), at = n("warn"), cr = n("crit");
  const geral = cr ? "crit" : at ? "warn" : "good";
  const frase = cr ? `${cr} ${cr === 1 ? "ponto pede" : "pontos pedem"} ação imediata` : at ? "Nada crítico, mas há pontos de atenção" : "Uso de IA saudável em todos os indicadores";
  const segs = ["good", "warn", "crit"].map(t => n(t) ? `<span class="${t}" style="flex:${n(t)}" title="${n(t)} ${SITUACAO[t].rotulo}"></span>` : "").join("");
  return `<div class="resumo-geral ${geral}">
      <div><span class="eyebrow">Situação geral</span><strong>${frase}</strong>
        <span class="muted small">${ok} ${ok === 1 ? "saudável" : "saudáveis"} · ${at} em atenção · ${cr} para agir agora, de ${itens.length} indicadores</span></div>
      <div class="placar" aria-hidden="true">${segs}</div></div>`;
}

/** Diagnóstico em semáforo e faixa de volume de uso. */
export default {
  secao: "kpis",
  disponivel: X => X.kpis != null,
  render(X, resumo) {
    const k = X.kpis, d = derivados(k);
    const itens = diagnostico(X, resumo);
    $("#diag-resumo").innerHTML = resumoGeral(itens);
    $("#kpis").innerHTML = itens.map(i => `<article class="diag ${i.t}">
        <header><span class="q">${i.q}</span>${selo(i.t)}</header>
        <div class="num"><span class="v">${i.v}</span><span class="d">${i.d}</span></div>
        ${i.r}<span class="muted small meta-txt">${i.meta}</span>
        <p class="acao"><b>O que fazer:</b> ${i.a}</p></article>`).join("");
    const mesesAtivos = (X.mensal || []).filter(m => m.ativos).length;
    $("#kpis-volume").innerHTML = [
      ["Requisições bem-sucedidas", big(k.requisicoes), `sucesso ${pct(k.sucesso)} · latência mediana ${nf(k.latencia)} ms`],
      ["Horas economizadas (est.)", big(Math.round(d.horas)) + " h", mesesAtivos ? `≈ ${nf(d.horas / Math.max(k.usuarios, 1) / mesesAtivos, 1)} h por usuário por mês` : "sobre as requisições bem-sucedidas"],
      ["Custo de IA no período", brl(k.custo), k.orcamento ? `orçamento ${brl(k.orcamento)}` : ""],
    ].map(cartao).join("");
  },
};

export { cartao };
