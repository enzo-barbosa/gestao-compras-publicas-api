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

export function senhaValidaMinima(senha: string): boolean {
  return senha.length >= 8
}

export function confirmaSenhaValida(senha: string, confirmacao: string): boolean {
  return senha.length > 0 && senha === confirmacao
}