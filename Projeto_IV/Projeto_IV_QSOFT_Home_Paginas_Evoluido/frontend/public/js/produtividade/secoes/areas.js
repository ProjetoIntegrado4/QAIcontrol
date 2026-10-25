import { $, big, brl, brlFull, css, esc, nf, pct } from "../formato.js";
import { horas } from "../preferencias.js";
import { graficoRadar } from "../componentes/graficoRadar.js";
import { celulaBarra } from "../componentes/barras.js";
import { tabela } from "../componentes/tabela.js";
import { barrasMeta } from "../componentes/visuais.js";
import { METAS, tom } from "./indicadores.js";

const tomAdocao = v => tom(v, METAS.adocao, METAS.adocaoCritica);
const COR = { good: "--good", warn: "--warn", crit: "--crit" };

const MEDIDAS = {
  adocao: ["Adoção (%)", (v, eixo) => pct(v, 0), { max: 100, meta: METAS.adocao, cor: v => css(COR[tomAdocao(v)] || "--s1") }],
  req: ["Requisições bem-sucedidas", v => big(v)],
  usuarios: ["Usuários de IA", v => nf(v)],
  req_usuario: ["Requisições por usuário", v => big(v)],
};
const MAX_AREAS = 12;
const MAX_BARRAS = 14;
let medida = "adocao";

/** Áreas com pelo menos 5 pessoas: percentuais de áreas minúsculas distorcem a leitura. */
const relevantes = areas => {
  const A = areas.filter(a => a.funcionarios >= 5);
  return A.length >= 3 ? A : areas;
};

function renderRadar(areas) {
  const [nome, fmt, opt = {}] = MEDIDAS[medida];
  const base = medida === "adocao" ? relevantes(areas) : areas;
  const todas = base.map(a => ({ k: a.area, v: a[medida] || 0 })).sort((a, b) => b.v - a.v);
  // Com muitas áreas, o radar fica com as maiores (por pessoas, na adoção; pelo valor, nas demais).
  const escolha = medida === "adocao" ? [...base].sort((a, b) => b.funcionarios - a.funcionarios).slice(0, MAX_AREAS).map(a => a.area)
    : todas.slice(0, MAX_AREAS).map(r => r.k);
  const ordem = areas.map(a => a.area).filter(a => escolha.includes(a));
  graficoRadar($("#ch-radar"), ordem.map(a => todas.find(s => s.k === a)), fmt, nome, opt);
  $("#radar-cap").textContent = nome + " · ranking";
  $("#radar-rank").innerHTML = todas.slice(0, 6).map(r => `<li><span>${esc(r.k)}</span><span class="v">${fmt(r.v)}</span></li>`).join("");
  $("#radar-note").textContent = medida === "adocao"
    ? `O anel tracejado é a meta de ${METAS.adocao}%: área com ponto dentro dele está abaixo da meta.${base.length > MAX_AREAS ? ` Mostra as ${MAX_AREAS} maiores áreas de ${base.length}.` : ""}`
    : todas.length > MAX_AREAS
      ? `O radar mostra as ${MAX_AREAS} áreas com maior valor de ${todas.length}; todas estão na tabela.`
      : "Cada eixo é uma área; quanto mais longe do centro, maior o uso.";
  document.querySelectorAll(".seg button").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.m === medida)));
}

/** Adoção de cada área contra a meta, das que mais usam às que menos usam. */
function renderAdocao(areas) {
  const ord = relevantes(areas).map(a => ({ k: a.area, v: a.adocao, tom: tomAdocao(a.adocao) })).sort((a, b) => b.v - a.v);
  const rows = ord.length > MAX_BARRAS
    ? [...ord.slice(0, MAX_BARRAS / 2), { sep: 1, k: `… mais ${ord.length - MAX_BARRAS} áreas na tabela …` }, ...ord.slice(-MAX_BARRAS / 2)]
    : ord;
  barrasMeta($("#bm-adocao"), rows, 100, METAS.adocao, v => pct(v, 0), `meta de adoção: ${METAS.adocao}% dos colaboradores`);
  const abaixo = ord.filter(a => a.v < METAS.adocao).length;
  $("#adocao-resumo").innerHTML = abaixo
    ? `<b>${abaixo} de ${ord.length}</b> áreas estão abaixo da meta.` : `Todas as ${ord.length} áreas estão na meta.`;
}

/** Radar de uso por área, adoção contra a meta e tabela completa por área. */
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
    renderAdocao(X.areas);
    const ar = X.areas.map(a => ({ ...a, horas: horas(a.req), cph: a.req && a.custo != null ? a.custo / horas(a.req) : null }));
    const mxA = Math.max(...ar.map(a => a.req_usuario), 1);
    tabela($("#t-areas"), [
      { k: "area", t: "Área" },
      { k: "adocao", t: "Adoção", f: v => celulaBarra(v, 100, pct(v, 0), `var(${COR[tomAdocao(v)] || "--s1"})`) },
      { k: "usuarios", t: "Usuários", n: 1, f: (v, r) => `${nf(v)}<span class="muted"> / ${nf(r.funcionarios)}</span>` },
      { k: "req_usuario", t: "Req. por usuário", f: v => celulaBarra(v, mxA, big(v), "var(--s3)") },
      { k: "horas", t: "Horas poupadas (est.)", n: 1, f: v => nf(Math.round(v)) },
      { k: "custo", t: "Custo", n: 1, f: v => brl(v) },
      { k: "cph", t: "Custo por hora poupada", n: 1, f: v => v == null ? "–" : brlFull(v) },
      { k: "trein", t: "Treinam. concluídos", n: 1, f: v => pct(v, 0) },
    ], ar, "adocao");
  },
};
