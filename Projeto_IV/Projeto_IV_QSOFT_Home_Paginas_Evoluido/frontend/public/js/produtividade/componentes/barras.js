import { esc } from "../formato.js";

/**
 * Barras horizontais: rows = [{ k: rótulo, v: valor, c?: cor }]. Sem linhas, mostra um aviso.
 * Larguras e cores são aplicadas pelo DOM depois de montar a marcação.
 */
export function barrasHorizontais(el, rows, fmt, color = "var(--s1)") {
  const mx = Math.max(...rows.map(r => r.v), 1);
  el.innerHTML = rows.length
    ? rows.map(r => `<div class="hb" title="${esc(r.k)}: ${esc(fmt(r.v))}"><span class="lab">${esc(r.k)}</span>
      <span class="row"><span class="fill"></span><span class="val">${fmt(r.v)}</span></span></div>`).join("")
    : `<p class="muted small vazio">Sem registros para esta empresa.</p>`;
  el.querySelectorAll(".fill").forEach((f, i) => {
    f.style.width = (rows[i].v / mx * 78).toFixed(1) + "%";
    f.style.background = rows[i].c || color;
  });
}

/** Célula de tabela com barra proporcional (preenchida por {@link aplicarBarras} depois de renderizar). */
export const celulaBarra = (v, max, txt, color = "var(--s1)") =>
  `<div class="bar" data-w="${Math.min(100, (v || 0) / (max || 1) * 100).toFixed(1)}" data-c="${color}"><span class="track"><span class="fill"></span></span><span class="mono small">${txt}</span></div>`;

export function aplicarBarras(el) {
  el.querySelectorAll(".bar[data-w]").forEach(b => {
    const f = b.querySelector(".fill");
    f.style.display = "block";
    f.style.width = b.dataset.w + "%";
    f.style.background = b.dataset.c;
  });
}
