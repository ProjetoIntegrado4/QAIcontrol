import { $, esc, nf } from "../formato.js";
import { tabela } from "../componentes/tabela.js";

/** Colaboradores com mais requisições bem-sucedidas na janela recente. */
export default {
  secao: "top",
  tabelas: ["#t-top"],
  render(X) {
    tabela($("#t-top"), [
      { k: "id", t: "Colaborador", f: v => `<span class="mono">${esc(v)}</span>` },
      { k: "cargo", t: "Cargo", f: (v, r) => `${esc(v)}<div class="muted small">${esc(r.senioridade)}</div>` },
      { k: "area", t: "Área" },
      { k: "req", t: "Requisições", n: 1, f: v => nf(v) },
      { k: "dias", t: "Dias com uso", n: 1, f: v => nf(v) },
      { k: "ferramentas", t: "Ferramentas", n: 1, f: v => nf(v) },
    ], X.top, "req");
  },
};
