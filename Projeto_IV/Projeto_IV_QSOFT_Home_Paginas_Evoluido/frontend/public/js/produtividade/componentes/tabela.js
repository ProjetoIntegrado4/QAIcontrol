import { esc } from "../formato.js";
import { aplicarBarras } from "./barras.js";

/**
 * Tabela ordenável pelo cabeçalho (clique ou Enter).
 * cols: [{ k: campo, t: título, n?: numérica, f?: (valor, linha) => html }].
 * A ordenação escolhida fica no elemento até {@link limparOrdenacao}.
 */
export function tabela(el, cols, rows, sortKey, dir = -1) {
  const st = el._sort || (el._sort = { k: sortKey, d: dir });
  const sorted = [...rows].sort((a, b) => {
    const x = a[st.k], y = b[st.k];
    return (typeof x === "string" ? x.localeCompare(y) : (x ?? -1e18) - (y ?? -1e18)) * st.d;
  });
  el.innerHTML = `<thead><tr>${cols.map(c => `<th class="${c.n ? "n" : ""}" data-k="${c.k}" aria-sort="${st.k === c.k ? (st.d < 0 ? "descending" : "ascending") : "none"}" tabindex="0">${c.t}</th>`).join("")}</tr></thead>
    <tbody>${sorted.map(r => `<tr>${cols.map(c => `<td class="${c.n ? "n" : ""}">${c.f ? c.f(r[c.k], r) : esc(r[c.k])}</td>`).join("")}</tr>`).join("")}</tbody>`;
  aplicarBarras(el);
  el.querySelectorAll("th").forEach(th => {
    const go = () => { const k = th.dataset.k; st.d = st.k === k ? -st.d : -1; st.k = k; tabela(el, cols, rows); };
    th.onclick = go;
    th.onkeydown = e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); go(); } };
  });
}

export function limparOrdenacao(...elementos) {
  elementos.forEach(el => { if (el) delete el._sort; });
}
