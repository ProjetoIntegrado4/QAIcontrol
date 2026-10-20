import { $, big, brl, brlFull, esc, nf, pct } from "../formato.js";
import { horas } from "../preferencias.js";
import { graficoRadar } from "../componentes/graficoRadar.js";
import { celulaBarra } from "../componentes/barras.js";
import { tabela } from "../componentes/tabela.js";

const MEDIDAS = {
  req: ["Requisições bem-sucedidas", v => big(v)],
  usuarios: ["Usuários de IA", v => nf(v)],
  req_usuario: ["Requisições por usuário", v => big(v)],
};
const MAX_AREAS = 12;
let medida = "req";

function renderRadar(areas) {
  const [nome, fmt] = MEDIDAS[medida];
  const todas = areas.map(a => ({ k: a.area, v: a[medida] || 0 })).sort((a, b) => b.v - a.v);
  const exibidas = todas.slice(0, MAX_AREAS), ordem = areas.map(a => a.area).filter(a => exibidas.some(s => s.k === a));
  graficoRadar($("#ch-radar"), ordem.map(a => exibidas.find(s => s.k === a)), fmt, nome);
  $("#radar-cap").textContent = nome + " · ranking";
  $("#radar-rank").innerHTML = exibidas.slice(0, 6).map(r => `<li><span>${esc(r.k)}</span><span class="v">${fmt(r.v)}</span></li>`).join("");
  $("#radar-note").textContent = todas.length > MAX_AREAS
    ? `O radar mostra as ${MAX_AREAS} áreas com maior valor de ${todas.length}; todas estão na tabela abaixo.`
    : "Cada eixo é uma área; quanto mais longe do centro, maior o uso.";
  document.querySelectorAll(".seg button").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.m === medida)));
}

/** Radar de uso por área e tabela de produtividade por área. */
export default {
  secao: "areas",
  tabelas: ["#t-areas"],
  iniciar(painelAtual) {
    document.querySelectorAll(".seg button").forEach(b => b.onclick = () => {
      medida = b.dataset.m;
      const X = painelAtual();
      if (X && X.areas) renderRadar(X.areas);
    });
  },
  render(X) {
    renderRadar(X.areas);
    const ar = X.areas.map(a => ({ ...a, horas: horas(a.req), cph: a.req && a.custo != null ? a.custo / horas(a.req) : null }));
    const mxA = Math.max(...ar.map(a => a.req_usuario), 1);
    tabela($("#t-areas"), [
      { k: "area", t: "Área" },
      { k: "adocao", t: "Adoção", f: v => celulaBarra(v, 100, pct(v, 0)) },
      { k: "usuarios", t: "Usuários", n: 1, f: (v, r) => `${nf(v)}<span class="muted"> / ${nf(r.funcionarios)}</span>` },
      { k: "req_usuario", t: "Req. por usuário", f: v => celulaBarra(v, mxA, big(v), "var(--s3)") },
      { k: "horas", t: "Horas poupadas (est.)", n: 1, f: v => nf(Math.round(v)) },
      { k: "custo", t: "Custo", n: 1, f: v => brl(v) },
      { k: "cph", t: "Custo por hora poupada", n: 1, f: v => v == null ? "–" : brlFull(v) },
      { k: "trein", t: "Treinam. concluídos", n: 1, f: v => pct(v, 0) },
    ], ar, "adocao");
  },
};
