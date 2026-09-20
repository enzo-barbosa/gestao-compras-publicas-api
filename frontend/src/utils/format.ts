import { apenasDigitos } from './validacao'

const SEPARADOR_MILHAR = /\B(?=(\d{3})+(?!\d))/g

export function formatarMoeda(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—'
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
}

export function formatarData(data: string | null | undefined): string {
  if (!data) return '—'
  return new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(new Date(data + (data.length === 10 ? 'T00:00:00Z' : '')))
}

export function formatarCompetencia(mes: number, ano: number): string {
  const nomes = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
  return `${nomes[mes - 1] ?? '?'}/${ano}`
}

export function mascaraMoeda(valor: string): string {
  const temDecimal = /[.,]/.test(valor)
  const digitos = apenasDigitos(valor).slice(0, 14)
  if (!digitos) return ''
  if (temDecimal) {
    const inteiros = digitos.slice(0, -2).replace(/^0+(?=\d)/, '') || '0'
    const centavos = digitos.slice(-2).padStart(2, '0')
    return `${inteiros.replace(SEPARADOR_MILHAR, '.')},${centavos}`
  }
  return digitos.replace(/^0+(?=\d)/, '').replace(SEPARADOR_MILHAR, '.')
}

export function valorDaMascaraMoeda(valor: string): number {
  if (!valor) return 0
  const [parteInteira, parteDecimal] = valor.split(',')
  const inteiros = apenasDigitos(parteInteira ?? '')
  const centavos = (parteDecimal === undefined ? '' : apenasDigitos(parteDecimal)).slice(0, 2)
  const numero = Number(`${inteiros || '0'}${centavos ? `.${centavos}` : ''}`)
  return Number.isFinite(numero) ? numero : 0
}

export function mascaraCnpj(valor: string): string {
  const digitos = apenasDigitos(valor).slice(0, 14)
  if (!digitos) return ''
  if (digitos.length <= 2) return digitos
  if (digitos.length <= 5) return `${digitos.slice(0, 2)}.${digitos.slice(2)}`
  if (digitos.length <= 8) return `${digitos.slice(0, 2)}.${digitos.slice(2, 5)}.${digitos.slice(5)}`
  if (digitos.length <= 12) return `${digitos.slice(0, 2)}.${digitos.slice(2, 5)}.${digitos.slice(5, 8)}/${digitos.slice(8)}`
  return `${digitos.slice(0, 2)}.${digitos.slice(2, 5)}.${digitos.slice(5, 8)}/${digitos.slice(8, 12)}-${digitos.slice(12)}`
}

export function mascaraNumeroEdital(valor: string): string {
  const limpo = valor.replace(/[^\d/]/g, '').replace(/\/+/g, '/')
  const [numero, ano] = limpo.split('/')
  const numeroEditado = (numero ?? '').slice(0, 4)
  if (ano === undefined) return numeroEditado
  return `${numeroEditado}/${(ano ?? '').slice(0, 4)}`
}

export function formatarStatusEmpenho(status: string): string {
  const rotulos: Record<string, string> = {
    EMPENHADO: 'Empenhado',
    LIQUIDADO: 'Liquidado',
    PAGO: 'Pago',
    ANULADO: 'Anulado',
  }
  return rotulos[status] ?? status
}

export function extrairMensagemErro(erro: unknown): string {
  const e = erro as { response?: { data?: { mensagem?: string; detalhes?: string[] } } }
  if (e?.response?.data) {
    if (e.response.data.detalhes && e.response.data.detalhes.length > 0) {
      return e.response.data.detalhes.join(' · ')
    }
    if (e.response.data.mensagem) return e.response.data.mensagem
  }
  if (erro instanceof Error && erro.message) return erro.message
  return 'Erro inesperado. Tente novamente.'
}

export function saudacaoBemVindo(nome: string | undefined): string {
  return `Bem-vindo(a), ${nome ?? 'você'}!`
}
