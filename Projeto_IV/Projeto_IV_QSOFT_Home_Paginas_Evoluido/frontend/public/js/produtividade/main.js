/*
 * Painel de Produtividade de IA: busca os dados no backend e desenha cada seção.
 * Para acrescentar uma seção, crie um módulo em ./secoes (com secao, render e, se preciso,
 * disponivel, tabelas e iniciar), inclua-o em SECOES e marque o HTML com data-secao.
 */
import { $ } from "./formato.js";
import { buscarPainel, buscarResumo } from "./api.js";
import { preferencias, salvar } from "./preferencias.js";
import { renderCabecalho, renderEmpresa, renderRodape } from "./cabecalho.js";
import { limparOrdenacao } from "./componentes/tabela.js";
import indicadores from "./secoes/indicadores.js";
import alertas from "./secoes/alertas.js";
import mensal from "./secoes/mensal.js";
import areas from "./secoes/areas.js";
import ferramentas from "./secoes/ferramentas.js";
import engenharia from "./secoes/engenharia.js";
import governanca from "./secoes/governanca.js";
import colaboradores from "./secoes/colaboradores.js";

const SECOES = [indicadores, alertas, mensal, areas, ferramentas, engenharia, governanca, colaboradores];

const dados = { resumo: null, paineis: {} };
const painelAtual = () => dados.paineis[preferencias.emp];

function estado(txt, erro) {
  const e = $("#estado");
  e.hidden = !txt;
  e.textContent = txt || "";
  e.classList.toggle("erro", !!erro);
}

/** Desenha as seções com dados; esconde as que vieram nulas (tabela ausente no banco ou sem registros). */
function render() {
  const X = painelAtual();
  if (!X || !dados.resumo) return;
  renderEmpresa(dados.resumo);
  for (const s of SECOES) {
    const el = document.querySelector(`[data-secao="${s.secao}"]`);
    const ok = s.disponivel ? s.disponivel(X) : X[s.secao] != null;
    if (el) el.hidden = !ok;
    if (ok) s.render(X, dados.resumo);
  }
  renderRodape(dados.resumo, X);
}

async function mostrarEmpresa() {
  const id = preferencias.emp;
  if (!dados.paineis[id]) estado("Carregando dados do banco…");
  try {
    if (!dados.paineis[id]) dados.paineis[id] = await buscarPainel(id);
    if (id !== preferencias.emp) return;
    estado("");
    render();
  } catch (e) {
    estado("Não foi possível carregar os dados: " + e.message, true);
  }
}

async function recarregar() {
  const b = $("#b-atualizar");
  b.disabled = true;
  estado("Carregando dados do banco…");
  try {
    dados.resumo = await buscarResumo();
    dados.paineis = {};
    renderCabecalho(dados.resumo);
    await mostrarEmpresa();
  } catch (e) {
    estado("Não foi possível carregar os dados: " + e.message, true);
  } finally {
    b.disabled = false;
  }
}

function iniciar() {
  const sel = $("#f-empresa");
  $("#f-min").value = preferencias.min;
  $("#f-hora").value = preferencias.hora;
  sel.onchange = () => {
    preferencias.emp = sel.value;
    limparOrdenacao(...SECOES.flatMap(s => s.tabelas || []).map(t => $(t)));
    salvar();
    mostrarEmpresa();
  };
  const numero = (id, chave) => $(id).addEventListener("input", e => {
    const v = parseFloat(e.target.value);
    if (v >= 0) { preferencias[chave] = v; salvar(); render(); }
  });
  numero("#f-min", "min");
  numero("#f-hora", "hora");
  SECOES.forEach(s => s.iniciar && s.iniciar(painelAtual));
  $("#b-atualizar").onclick = recarregar;

  let rt;
  window.addEventListener("resize", () => { clearTimeout(rt); rt = setTimeout(render, 150); });
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", render);
  new MutationObserver(render).observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
  recarregar();
}

iniciar();
