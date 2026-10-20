import { $, big, brl, esc, nf, pct } from "../formato.js";
import { barrasHorizontais } from "../componentes/barras.js";
import { tabela } from "../componentes/tabela.js";

/** Requisições, custo, licenças e conformidade por ferramenta. */
export default {
  secao: "ferramentas",
  tabelas: ["#t-ferr"],
  render(X) {
    barrasHorizontais($("#hb-ferr"), X.ferramentas.map(f => ({ k: f.nome, v: f.req })), big);
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
