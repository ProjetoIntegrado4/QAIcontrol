/* Acesso ao backend (GET /api/produtividade/...), que consulta o PostgreSQL a cada carga. */

async function get(caminho) {
  const r = await fetch(caminho, { credentials: "same-origin", headers: { Accept: "application/json" } });
  if (r.status === 401) {
    window.location.replace("./login.html");
    throw new Error("Sessão expirada");
  }
  if (!r.ok) {
    let m = "";
    try { m = (await r.json()).message || ""; } catch (e) { /* corpo sem JSON */ }
    throw new Error(m || `Erro ${r.status} ao consultar ${caminho}`);
  }
  return r.json();
}

/** Empresas, período da base, qualidade dos dados e mapeamento de tabelas. */
export const buscarResumo = () => get("/api/produtividade/resumo");

/** Painel de uma empresa (id) ou de todas ("ALL"). */
export const buscarPainel = id => get("/api/produtividade/empresas/" + encodeURIComponent(id));
