export function apenasDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

const PESOS_PRIMEIRO_DV = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
const PESOS_SEGUNDO_DV = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]

function digitoVerificador(base: string, pesos: number[]): number {
  let soma = 0
  for (let i = 0; i < base.length; i += 1) {
    soma += Number(base[i]) * pesos[i]!
  }
  const resto = soma % 11
  return resto < 2 ? 0 : 11 - resto
}

export function cnpjValido(cnpj: string): boolean {
  const digitos = apenasDigitos(cnpj)
  if (digitos.length !== 14) return false
  if (/^(\d)\1{13}$/.test(digitos)) return false
  const primeiro = digitoVerificador(digitos.slice(0, 12), PESOS_PRIMEIRO_DV)
  const segundo = digitoVerificador(digitos.slice(0, 13), PESOS_SEGUNDO_DV)
  return Number(digitos[12]) === primeiro && Number(digitos[13]) === segundo
}

export function dataEncerramentoValida(abertura: string, encerramento: string | null | undefined): boolean {
  if (!encerramento) return true
  return encerramento >= abertura
}

function hojeISO(): string {
  const hoje = new Date()
  const mes = String(hoje.getMonth() + 1).padStart(2, '0')
  const dia = String(hoje.getDate()).padStart(2, '0')
  return `${hoje.getFullYear()}-${mes}-${dia}`
}

export function dataFuturaOuHoje(data: string): boolean {
  if (!data) return false
  return data >= hojeISO()
}

export function senhaValidaMinima(senha: string): boolean {
  return senha.length >= 8
}

export function confirmaSenhaValida(senha: string, confirmacao: string): boolean {
  return senha.length > 0 && senha === confirmacao
}

export const NUMERO_EDITAL_RE = /^\d{1,4}\/\d{4}$/

export function numeroEditalValido(valor: string): boolean {
  return valor.length > 0 && NUMERO_EDITAL_RE.test(valor)
}