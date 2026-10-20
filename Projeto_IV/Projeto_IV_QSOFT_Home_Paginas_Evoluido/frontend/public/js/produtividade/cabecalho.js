import { $, esc, mlab, nf } from "./formato.js";
import { preferencias } from "./preferencias.js";

/** Período da base, lista de empresas e textos que dependem da configuração do backend. */
export function renderCabecalho(resumo) {
  const p = resumo.periodo;
  $("#periodo").textContent = p.length
    ? `Base de uso de IA · ${mlab(p[0])} a ${mlab(p[1])} · consultado em ${resumo.atualizado_em}`
    : "Base de uso de IA";
  const sel = $("#f-empresa");
  sel.innerHTML = resumo.empresas.map(e => `<option value="${esc(e.id)}">${e.id === "ALL" ? "" : esc(e.id) + " · "}${esc(e.nome)}</option>`).join("");
  if (!resumo.empresas.some(e => e.id === preferencias.emp)) preferencias.emp = "ALL";
  sel.value = preferencias.emp;
  document.querySelectorAll(".janela-dias").forEach(s => s.textContent = resumo.janela_dias);
}

/** Linha ao lado do título com setor e porte da empresa escolhida. */
export function renderEmpresa(resumo) {
  const emp = resumo.empresas.find(e => e.id === preferencias.emp);
  $("#empresa-info").textContent = !emp ? "" : emp.id === "ALL" ? emp.setor + " na base" : `${emp.setor} · porte ${emp.porte}`;
}

const NOMES_PARTES = {
  kpis: "Indicadores", mensal: "Evolução mensal", areas: "Áreas", ferramentas: "Ferramentas",
  eng: "Entrega de software", gov: "Governança", top: "Colaboradores mais ativos",
};
const nomeParte = p => { const [s, sub] = p.split("."); return (NOMES_PARTES[s] || s) + (sub ? ` (${sub})` : ""); };
const mono = t => `<span class="mono">${esc(t)}</span>`;

/** Notas de método, qualidade dos dados e o que ficou de fora por faltar no banco. */
export function renderRodape(resumo, painel) {
  const q = resumo.qualidade, t = resumo.tabelas || {};
  const faltas = { ...(resumo.indisponiveis || {}), ...((painel && painel.indisponiveis) || {}) };
  const qualidade = [
    q.lead_negativo != null && `${nf(q.lead_negativo)} pull requests têm data de merge anterior à de criação e foram excluídos do tempo até o merge`,
    q.review_imputado != null && `${nf(q.review_imputado)} têm contagem de revisões fracionária (provável preenchimento pela média)`,
    q.uso_sem_cc != null && `${nf(q.uso_sem_cc)} de ${nf(q.linhas_uso)} registros de uso não têm centro de custo`,
  ].filter(Boolean);
  const itens = [
    `Os dados vêm do banco PostgreSQL do projeto (tabelas ${Object.values(t).map(mono).join(", ")}, conforme o mapeamento em <span class="mono">produtividade.yml</span>). Cada carga consulta o banco, então registros novos aparecem ao clicar em "Atualizar dados".`,
    `<b>Adoção</b>: colaboradores ativos com ao menos uma requisição bem-sucedida no período, sobre o total de colaboradores ativos do cadastro.`,
    `<b>Horas economizadas</b> e <b>retorno</b> são estimativas: requisições bem-sucedidas × minutos poupados por requisição × custo da hora. A base não mede tempo poupado.`,
    t.faturamento && `<b>Custo</b> é a soma do custo faturado da tabela ${mono(t.faturamento)}, em reais.`,
    `<b>Fora da política</b>: uso de ferramenta sem aprovação para a empresa, ou por área que não está entre as autorizadas.`,
    qualidade.length && `Qualidade dos dados: ${qualidade.join("; ")}.`,
    Object.keys(faltas).length && `<b>Fora do painel por falta de dados no banco</b>: ${Object.entries(faltas).map(([p, m]) => `${esc(nomeParte(p))}, ${esc(m)}`).join("; ")}.`,
  ].filter(Boolean);
  $("#rodape").innerHTML = `<h3>Como os números foram calculados</h3><ul>${itens.map(i => `<li>${i}</li>`).join("")}</ul>`;
}
