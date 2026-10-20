/* Escolhas do usuário (empresa e premissas de horas), guardadas no navegador. */

const CHAVE = "painel-ia";

export const preferencias = { emp: "ALL", min: 1.5, hora: 55 };
try { Object.assign(preferencias, JSON.parse(localStorage.getItem(CHAVE) || "{}")); } catch (e) { /* sem armazenamento */ }

export function salvar() {
  try { localStorage.setItem(CHAVE, JSON.stringify(preferencias)); } catch (e) { /* sem armazenamento */ }
}

/** Horas economizadas estimadas: requisições x minutos poupados por requisição. */
export const horas = req => req * preferencias.min / 60;
