/*
 * Textos de exibição para valores que vêm do banco. Um valor que não estiver aqui aparece como está,
 * então valores novos no banco continuam funcionando sem mudar o código.
 */

const SITUACAO_TREINAMENTO = {
  Concluido: "Concluído",
  "Em Andamento": "Em andamento",
  "Nao Iniciado": "Não iniciado",
  Abandonado: "Abandonado",
};

const COR_SITUACAO_TREINAMENTO = {
  "Em Andamento": "var(--s1)",
  "Nao Iniciado": "var(--ref)",
  Abandonado: "var(--crit)",
};

export const rotuloTreinamento = s => SITUACAO_TREINAMENTO[s] || s;

/** Cor da barra: verde para os status configurados como "concluído" no backend. */
export const corTreinamento = (s, concluidos) => concluidos.includes(s) ? "var(--good)" : COR_SITUACAO_TREINAMENTO[s] || "var(--s1)";
