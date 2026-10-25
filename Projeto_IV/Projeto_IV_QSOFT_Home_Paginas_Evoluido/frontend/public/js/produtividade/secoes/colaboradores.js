import { $, esc, nf } from "../formato.js";
import { tabela } from "../componentes/tabela.js";
import { celulaBarra } from "../componentes/barras.js";

/** Colaboradores com mais requisições bem-sucedidas na janela recente. */
export default {
  secao: "top",
  tabelas: ["#t-top"],
  render(X) {
    const mx = Math.max(...X.top.map(t => t.req), 1);
    tabela($("#t-top"), [
      { k: "nome", t: "Colaborador", f: (v, r) => `${v ? esc(v) : ""}<div class="muted small mono">${esc(r.id)}</div>` },
      { k: "cargo", t: "Cargo", f: (v, r) => `${esc(v)}<div class="muted small">${esc(r.senioridade)}</div>` },
      { k: "area", t: "Área" },
      { k: "req", t: "Requisições", n: 1, f: v => celulaBarra(v, mx, nf(v)) },
      { k: "dias", t: "Dias com uso", n: 1, f: v => nf(v) },
      { k: "ferramentas", t: "Ferramentas", n: 1, f: v => nf(v) },
    ], X.top, "req");
  },
};
