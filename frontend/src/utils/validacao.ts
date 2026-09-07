export function apenasDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

export function cnpjValido(cnpj: string): boolean {
  return apenasDigitos(cnpj).length === 14
}

export function dataEncerramentoValida(abertura: string, encerramento: string | null | undefined): boolean {
  if (!encerramento) return true
  return encerramento >= abertura
}