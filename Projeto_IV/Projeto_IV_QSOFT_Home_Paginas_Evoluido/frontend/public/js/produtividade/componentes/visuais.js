import { esc, nf } from "../formato.js";

/** Rótulo e ícone de cada situação: a cor nunca aparece sozinha. */
export const SITUACAO = {
  good: { rotulo: "Saudável", icone: "✓" },
  warn: { rotulo: "Atenção", icone: "!" },
  crit: { rotulo: "Agir agora", icone: "✕" },
};

export const selo = t => t && SITUACAO[t]
  ? `<span class="selo ${t}"><i aria-hidden="true">${SITUACAO[t].icone}</i>${SITUACAO[t].rotulo}</span>` : "";

/**
 * Régua horizontal com faixas de referência: mostra onde o valor está entre 0 e max
 * e marca a meta com um traço. inv = quanto menor, melhor.
 */
export function regua(v, max, meta, tom, rotuloMeta) {
  const p = x => Math.max(0, Math.min(100, (x || 0) / max * 100)).toFixed(1);
  return `<div class="regua" role="img" aria-label="${esc(rotuloMeta || "")}">
    <span class="trilho"><span class="valor ${tom || ""}" style="width:${p(v)}%"></span></span>
    ${meta != null ? `<span class="meta" style="left:${p(meta)}%" title="${esc(rotuloMeta || "")}"></span>` : ""}
  </div>`;
}

/**
 * Barras horizontais com linha de meta e cor pela situação de cada linha.
 * rows = [{ k, v, tom }]; o texto do valor fica sempre visível.
 */
export function barrasMeta(el, rows, max, meta, fmt, rotuloMeta) {
  const p = x => Math.max(0, Math.min(100, x / max * 100)).toFixed(1);
  el.innerHTML = rows.length ? rows.map(r => r.sep
    ? `<div class="bm-sep">${esc(r.k)}</div>`
    : `<div class="bm" title="${esc(r.k)}: ${esc(fmt(r.v))}">
      <span class="lab">${esc(r.k)}</span>
      <span class="trilho"><span class="valor ${r.tom || ""}" style="width:${p(r.v)}%"></span>
        <span class="meta" style="left:${p(meta)}%"></span></span>
      <span class="val">${fmt(r.v)}${r.tom === "crit" ? ' <i class="ic crit" aria-label="crítico">✕</i>' : r.tom === "warn" ? ' <i class="ic warn" aria-label="atenção">!</i>' : ""}</span></div>`).join("")
    + `<p class="muted small bm-legenda"><span class="traco"></span>${esc(rotuloMeta)}</p>`
    : `<p class="muted small vazio">Sem registros para esta empresa.</p>`;
}

/** Barras empilhadas de duas partes (ex.: licenças em uso x ociosas). rows = [{ k, a, b }]. */
export function barrasEmpilhadas(el, rows, nomes, cores, fmt) {
  const mx = Math.max(...rows.map(r => r.a + r.b), 1);
  el.innerHTML = rows.length
    ? `<div class="legend">${nomes.map((n, i) => `<span><i style="background:${cores[i]}"></i>${esc(n)}</span>`).join("")}</div>`
      + rows.map(r => `<div class="hb" title="${esc(r.k)}: ${fmt(r.a)} ${esc(nomes[0].toLowerCase())}, ${fmt(r.b)} ${esc(nomes[1].toLowerCase())}">
        <span class="lab">${esc(r.k)}</span>
        <span class="row"><span class="pilha" style="width:${(78 * (r.a + r.b) / mx).toFixed(1)}%">
          <span style="flex:${r.a};background:${cores[0]}"></span><span style="flex:${r.b};background:${cores[1]}"></span></span>
          <span class="val">${fmt(r.b)} / ${fmt(r.a + r.b)}</span></span></div>`).join("")
    : `<p class="muted small vazio">Sem registros para esta empresa.</p>`;
}

/** Rosca com legenda; o centro mostra o destaque (ex.: % concluído). rows = [{ k, v, c }]. */
export function rosca(el, rows, centro, legendaCentro) {
  const total = rows.reduce((s, r) => s + r.v, 0);
  if (!total) { el.innerHTML = `<p class="muted small vazio">Sem registros para esta empresa.</p>`; return; }
  const R = 54, C = 2 * Math.PI * R, gap = rows.filter(r => r.v).length > 1 ? 2 : 0;
  let acc = 0, arcos = "";
  rows.forEach(r => {
    if (!r.v) return;
    const len = r.v / total * C;
    arcos += `<circle r="${R}" cx="70" cy="70" fill="none" stroke="${r.c}" stroke-width="18"
      stroke-dasharray="${Math.max(0, len - gap)} ${C}" stroke-dashoffset="${-acc}" transform="rotate(-90 70 70)"><title>${esc(r.k)}: ${nf(r.v)}</title></circle>`;
    acc += len;
  });
  el.innerHTML = `<div class="rosca"><svg viewBox="0 0 140 140" role="img" aria-label="${esc(legendaCentro)}">${arcos}
      <text x="70" y="70" text-anchor="middle" class="rosca-v">${esc(centro)}</text>
      <text x="70" y="88" text-anchor="middle" class="rosca-l">${esc(legendaCentro)}</text></svg>
    <ul class="rosca-leg">${rows.map(r => `<li><i style="background:${r.c}"></i><span>${esc(r.k)}</span><b>${nf(r.v)}</b><span class="muted">${nf(100 * r.v / total, 0)}%</span></li>`).join("")}</ul></div>`;
}
