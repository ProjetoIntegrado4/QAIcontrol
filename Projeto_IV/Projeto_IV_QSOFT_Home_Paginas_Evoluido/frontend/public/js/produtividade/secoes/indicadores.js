import { $, big, brl, nf, pct } from "../formato.js";
import { horas, preferencias } from "../preferencias.js";

/** Classifica um valor: "good", "warn" ou "crit" (inv = quanto menor, melhor). */
export const tom = (v, good, bad, inv) => v == null ? "" : (inv ? v <= good : v >= good) ? "good" : (inv ? v >= bad : v <= bad) ? "crit" : "warn";

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

/** Faixa de indicadores do topo. */
export default {
  secao: "kpis",
  render(X, resumo) {
    const k = X.kpis, d = derivados(k);
    const mesesAtivos = (X.mensal || []).filter(m => m.ativos).length;
    const tiles = [
      ["Adoção", pct(k.adocao, 0), `${nf(k.usuarios)} de ${nf(k.funcionarios)} colaboradores ativos usaram IA`, tom(k.adocao, 70, 40), k.adocao >= 70 ? "alta" : k.adocao <= 40 ? "baixa" : "moderada"],
      ["Requisições bem-sucedidas", big(k.requisicoes), `taxa de sucesso ${pct(k.sucesso)} · latência mediana ${nf(k.latencia)} ms`],
      ["Horas economizadas (est.)", big(Math.round(d.horas)) + " h", mesesAtivos ? `≈ ${nf(d.horas / Math.max(k.usuarios, 1) / mesesAtivos, 1)} h por usuário por mês` : "estimativa sobre as requisições bem-sucedidas"],
      ["Retorno estimado", d.roi == null ? "–" : nf(d.roi, 1) + "×", `${brl(d.valor)} em tempo poupado para ${brl(k.custo)} de custo`, d.roi == null ? "" : tom(d.roi, 2, 1), d.roi >= 2 ? "positivo" : d.roi < 1 ? "abaixo do custo" : "marginal"],
      ["Custo x orçamento", pct(d.usoOrcamento, 0), `${brl(k.custo)} de ${brl(k.orcamento)} orçados`, tom(d.usoOrcamento, 90, 100, true), d.usoOrcamento > 100 ? "acima" : d.usoOrcamento > 90 ? "no limite" : "dentro"],
      ["Licenças ociosas", nf(k.lic_ociosas), `${pct(d.ociosidade, 0)} das ${nf(k.lic_ativas)} licenças ativas sem uso em ${resumo.janela_dias} dias`, tom(d.ociosidade, 15, 35, true), d.ociosidade > 35 ? "revisar" : d.ociosidade > 15 ? "atenção" : "ok"],
    ];
    $("#kpis").innerHTML = tiles.map(cartao).join("");
  },
};

export { cartao };
