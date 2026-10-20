/** Passo "redondo" de eixo (1, 2, 2,5, 5 ou 10 vezes uma potência de 10) próximo de r. */
export function nice(r) {
  const p = Math.pow(10, Math.floor(Math.log10(r || 1))), f = r / p;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10) * p;
}
