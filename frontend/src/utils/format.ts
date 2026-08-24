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
  return 'Erro inesperado. Tente novamente.'
}
