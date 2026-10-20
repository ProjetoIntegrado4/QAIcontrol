import { css, esc, mlab } from "../formato.js";
import { nice } from "./escala.js";

/**
 * Gráfico de linhas em SVG, com um eixo e dica ao passar o mouse.
 * series: [{ name, color, values, area?, dash? }]; fmt(v, eixo) formata os valores.
 * opt: { h: altura, min: piso do eixo, fit: ajusta o eixo aos dados, aria: descrição }.
 */
export function graficoLinhas(el, labels, series, fmt, opt = {}) {
  const W = Math.max(300, Math.round(el.clientWidth || 600)), H = opt.h || 210, L = 52, R = 12, T = 12, B = 26;
  const all = series.flatMap(s => s.values).filter(v => v != null);
  let lo = opt.min ?? 0, hi = Math.max(...all, 1);
  if (opt.min == null && opt.fit) { lo = Math.min(...all); const pad = (hi - lo) * .2 || 1; lo = Math.max(0, lo - pad); hi = hi + pad; }
  const step = nice((hi - lo) / 4); hi = Math.ceil(hi / step) * step; lo = Math.floor(lo / step) * step;
  const x = i => L + (labels.length < 2 ? 0 : i * (W - L - R) / (labels.length - 1));
  const y = v => T + (H - T - B) * (1 - (v - lo) / (hi - lo));
  let g = "";
  for (let v = lo; v <= hi + 1e-9; v += step) g += `<line class="grid-l" x1="${L}" x2="${W - R}" y1="${y(v)}" y2="${y(v)}"/><text x="${L - 8}" y="${y(v) + 4}" text-anchor="end">${fmt(v, true)}</text>`;
  const every = labels.length > 8 ? 2 : 1;
  labels.forEach((l, i) => { if (i % every === 0) g += `<text x="${x(i)}" y="${H - 6}" text-anchor="middle">${mlab(l)}</text>`; });
  series.forEach(s => {
    let d = "", pen = false;
    s.values.forEach((v, i) => { if (v == null) { pen = false; return; } d += (pen ? "L" : "M") + x(i) + " " + y(v); pen = true; });
    if (s.area) {
      const pts = s.values.map((v, i) => v == null ? null : [x(i), y(v)]).filter(Boolean);
      if (pts.length) g += `<path d="M${pts[0][0]} ${y(lo)} ${pts.map(p => "L" + p[0] + " " + p[1]).join(" ")} L${pts.at(-1)[0]} ${y(lo)}Z" fill="${s.color}" opacity=".12"/>`;
    }
    g += `<path d="${d}" fill="none" stroke="${s.color}" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" ${s.dash ? 'stroke-dasharray="5 4"' : ""}/>`;
    const li = s.values.map((v, i) => v == null ? -1 : i).filter(i => i >= 0).at(-1);
    if (li != null && !s.dash) g += `<circle cx="${x(li)}" cy="${y(s.values[li])}" r="4" fill="${s.color}" stroke="${css("--surface")}" stroke-width="2"/>`;
  });
  g += `<line id="xh" x1="0" x2="0" y1="${T}" y2="${H - B}" stroke="${css("--ink-3")}" stroke-width="1" opacity="0"/>`;
  g += `<rect x="${L}" y="${T}" width="${W - L - R}" height="${H - T - B}" fill="transparent" class="hit"/>`;
  el.innerHTML = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="${esc(opt.aria || "")}">${g}</svg><div class="tip" hidden></div>`;
  const svg = el.querySelector("svg"), tip = el.querySelector(".tip"), xh = svg.querySelector("#xh");
  const move = ev => {
    const r = svg.getBoundingClientRect(), px = (ev.clientX - r.left) * W / r.width;
    const i = Math.max(0, Math.min(labels.length - 1, Math.round((px - L) / ((W - L - R) / Math.max(1, labels.length - 1)))));
    xh.setAttribute("x1", x(i)); xh.setAttribute("x2", x(i)); xh.setAttribute("opacity", ".5");
    tip.innerHTML = `<div>${mlab(labels[i])}</div>` + series.map(s => `<div>${esc(s.name)}: <b>${fmt(s.values[i])}</b></div>`).join("");
    tip.hidden = false; tip.style.left = (x(i) * r.width / W) + "px"; tip.style.top = (T * r.height / H + 4) + "px";
  };
  svg.addEventListener("pointermove", move);
  svg.addEventListener("pointerleave", () => { tip.hidden = true; xh.setAttribute("opacity", "0"); });
}
