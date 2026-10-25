import { $, big, brl, esc, nf, pct } from "../formato.js";
import { barrasHorizontais } from "../componentes/barras.js";
import { tabela } from "../componentes/tabela.js";
import { barrasEmpilhadas } from "../componentes/visuais.js";

/** Requisições, custo, licenças e conformidade por ferramenta. */
export default {
  secao: "ferramentas",
  tabelas: ["#t-ferr"],
  render(X) {
    // Os gráficos mostram as 8 principais; a tabela recolhida traz todas.
    const MAX = 8, F = [...X.ferramentas].sort((a, b) => b.req - a.req);
    barrasHorizontais($("#hb-ferr"), F.slice(0, MAX).map(f => ({ k: f.nome, v: f.req })), big);
    barrasHorizontais($("#hb-custo"), [...F].filter(f => f.custo != null).sort((a, b) => b.custo - a.custo).slice(0, MAX).map(f => ({ k: f.nome, v: f.custo })), brl, "var(--s2)");
    const L = F.filter(f => f.lic_ativas).sort((a, b) => b.lic_ociosas - a.lic_ociosas).slice(0, MAX);
    barrasEmpilhadas($("#hb-lic"), L.map(f => ({ k: f.nome, a: f.lic_ativas - f.lic_ociosas, b: f.lic_ociosas })),
      ["Em uso", "Ociosas"], ["var(--s1)", "var(--ref)"], v => nf(v));
    tabela($("#t-ferr"), [
      { k: "nome", t: "Ferramenta", f: (v, r) => `${esc(v)}<div class="muted small">${esc(r.categoria)}</div>` },
      { k: "usuarios", t: "Usuários", n: 1, f: v => nf(v) },
      { k: "custo", t: "Custo", n: 1, f: v => brl(v) },
      { k: "cpm", t: "R$ / mil req.", n: 1, f: v => v == null ? "–" : nf(v, 2) },
      { k: "ociosas", t: "Licenças ociosas", n: 1, f: (v, r) => r.lic_ativas ? `${nf(r.lic_ociosas)}<span class="muted"> / ${nf(r.lic_ativas)}</span>` : "–" },
      { k: "fora", t: "Fora da política", n: 1, f: v => v == null ? "–" : v > 0 ? `<span class="pill ${v > 5 ? "crit" : "warn"}">${pct(v)}</span>` : `<span class="muted">0%</span>` },
    ], X.ferramentas.map(f => ({ ...f, cpm: f.req && f.custo != null ? f.custo / f.req * 1000 : null, ociosas: f.lic_ociosas })), "custo");
  },
};
