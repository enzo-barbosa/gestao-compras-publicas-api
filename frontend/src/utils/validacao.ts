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

export const CONVITE_CODIGO_RE = /^[A-Za-z0-9][A-Za-z0-9-]*$/

export function codigoConviteValido(codigo: string): boolean {
  return codigo.length >= 4 && codigo.length <= 24 && CONVITE_CODIGO_RE.test(codigo)
}

const CARACTERES_CONVITE = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

export function gerarCodigoConvite(tamanho = 8): string {
  const bytes = new Uint32Array(tamanho)
  crypto.getRandomValues(bytes)
  let codigo = ''
  for (let i = 0; i < tamanho; i += 1) {
    codigo += CARACTERES_CONVITE[bytes[i]! % CARACTERES_CONVITE.length]
  }
  return codigo
}