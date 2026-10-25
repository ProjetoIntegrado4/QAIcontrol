import { css, esc } from "../formato.js";
import { nice } from "./escala.js";

/**
 * Gráfico de radar (teia) de uma série: rows = [{ k: rótulo do eixo, v: valor }].
 * opt: { max: topo fixo da escala, meta: valor de referência desenhado como anel tracejado, cor: v => cor do ponto }.
 */
export function graficoRadar(el, rows, fmt, name, opt = {}) {
  const W = Math.max(300, Math.min(560, Math.round(el.clientWidth || 520))), H = Math.round(W * .82), cx = W / 2, cy = H / 2;
  const R = Math.min(W, H) / 2 - (W < 420 ? 52 : 70), n = rows.length, mx = Math.max(...rows.map(r => r.v), 1), top = opt.max || nice(mx / 4) * 4;
  const ang = i => -Math.PI / 2 + i * 2 * Math.PI / n, pt = (i, f) => [cx + Math.cos(ang(i)) * R * f, cy + Math.sin(ang(i)) * R * f];
  let g = "";
  [.25, .5, .75, 1].forEach(f => {
    g += `<polygon class="radar-ring" points="${rows.map((_, i) => pt(i, f).join(",")).join(" ")}"/>`;
    const a = -Math.PI / 2 - Math.PI / n;
    g += `<text x="${cx + Math.cos(a) * R * f}" y="${cy + Math.sin(a) * R * f + 4}" text-anchor="middle" class="radar-tick">${fmt(top * f, true)}</text>`;
  });
  rows.forEach((r, i) => {
    const [x, y] = pt(i, 1), [lx, ly] = pt(i, 1.1), c = Math.cos(ang(i));
    const lim = W < 420 ? 13 : 22, lab = r.k.length > lim ? r.k.slice(0, lim - 1) + "…" : r.k;
    g += `<line class="radar-axis" x1="${cx}" y1="${cy}" x2="${x}" y2="${y}"/>`;
    g += `<text class="radar-lab" x="${lx}" y="${ly + 4}" text-anchor="${Math.abs(c) < .2 ? "middle" : c > 0 ? "start" : "end"}">${esc(lab)}</text>`;
  });
  if (opt.meta != null) {
    g += `<polygon class="radar-meta" points="${rows.map((_, i) => pt(i, opt.meta / top).join(",")).join(" ")}"/>`;
  }
  const pts = rows.map((r, i) => pt(i, r.v / top));
  g += `<polygon points="${pts.map(p => p.join(",")).join(" ")}" fill="${css("--s1")}" fill-opacity=".16" stroke="${css("--s1")}" stroke-width="2" stroke-linejoin="round"/>`;
  pts.forEach((p, i) => g += `<circle cx="${p[0]}" cy="${p[1]}" r="4.5" fill="${opt.cor ? opt.cor(rows[i].v) : css("--s1")}" stroke="${css("--surface")}" stroke-width="2"/><circle data-i="${i}" cx="${p[0]}" cy="${p[1]}" r="14" fill="transparent" class="radar-hit"/>`);
  el.innerHTML = `<svg class="radar" viewBox="0 0 ${W} ${H}" role="img" aria-label="Gráfico de radar: ${esc(name)} por área">${g}</svg><div class="tip" hidden></div>`;
  const svg = el.querySelector("svg"), tip = el.querySelector(".tip");
  svg.style.maxWidth = W + "px";
  svg.querySelectorAll("circle[data-i]").forEach(c => {
    const show = () => {
      const i = +c.dataset.i, r = svg.getBoundingClientRect(), er = el.getBoundingClientRect(), s = r.width / W;
      tip.innerHTML = `<div>${esc(rows[i].k)}</div><div>${esc(name)}: <b>${fmt(rows[i].v)}</b></div>`; tip.hidden = false;
      tip.style.left = (r.left - er.left + pts[i][0] * s) + "px"; tip.style.top = (r.top - er.top + pts[i][1] * s) + "px";
    };
    c.addEventListener("pointerenter", show); c.addEventListener("focus", show);
    c.addEventListener("pointerleave", () => tip.hidden = true);
  });
}
