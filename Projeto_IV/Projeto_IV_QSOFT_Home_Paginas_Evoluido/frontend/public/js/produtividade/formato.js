/* Formatação de números, datas e texto para o painel (pt-BR). Valores nulos aparecem como "–". */

export const nf = (v, d = 0) => v == null ? "–" : v.toLocaleString("pt-BR", { minimumFractionDigits: d, maximumFractionDigits: d });
export const big = v => v == null ? "–" : v >= 1e9 ? nf(v / 1e9, 1) + " bi" : v >= 1e6 ? nf(v / 1e6, 1) + " mi" : v >= 1e4 ? nf(v / 1e3, 0) + " mil" : nf(v);
export const brl = v => v == null ? "–" : "R$ " + big(v);
export const brlFull = v => v == null ? "–" : v.toLocaleString("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });
export const pct = (v, d = 1) => v == null ? "–" : nf(v, d) + "%";

const MES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
/** "2025-06" -> "jun/25" */
export const mlab = m => MES[+m.slice(5) - 1] + "/" + m.slice(2, 4);

export const esc = s => String(s ?? "").replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

/** Valor atual de uma variável CSS do tema (cores do Estilo.css). */
export const css = n => getComputedStyle(document.documentElement).getPropertyValue(n).trim();

export const $ = s => document.querySelector(s);
