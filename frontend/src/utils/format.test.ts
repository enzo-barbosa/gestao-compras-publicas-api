import { describe, expect, it } from 'vitest'
import { extrairMensagemErro, formatarCompetencia, formatarData, formatarMoeda, formatarStatusEmpenho } from './format'

const moeda = (valor: number): string => formatarMoeda(valor).replace(/\u00A0/g, ' ')

describe('formatarMoeda', () => {
  it('formata valores em reais', () => {
    expect(moeda(1234.5)).toBe('R$ 1.234,50')
  })

  it('formata zero', () => {
    expect(moeda(0)).toBe('R$ 0,00')
  })

  it('retorna travessão para valores nulos', () => {
    expect(formatarMoeda(null)).toBe('—')
    expect(formatarMoeda(undefined)).toBe('—')
  })
})

describe('formatarData', () => {
  it('formata datas no padrão pt-BR', () => {
    expect(formatarData('2026-03-05')).toBe('05/03/2026')
    expect(formatarData('2026-11-30')).toBe('30/11/2026')
  })

  it('retorna travessão para datas vazias ou nulas', () => {
    expect(formatarData('')).toBe('—')
    expect(formatarData(null)).toBe('—')
    expect(formatarData(undefined)).toBe('—')
  })
})

describe('formatarCompetencia', () => {
  it('formata mês e ano com nome abreviado do mês', () => {
    expect(formatarCompetencia(3, 2026)).toBe('mar/2026')
    expect(formatarCompetencia(12, 2025)).toBe('dez/2025')
  })

  it('trata mês fora do intervalo com interrogação', () => {
    expect(formatarCompetencia(13, 2026)).toBe('?/2026')
    expect(formatarCompetencia(0, 2026)).toBe('?/2026')
  })
})

describe('formatarStatusEmpenho', () => {
  it('traduz os status conhecidos', () => {
    expect(formatarStatusEmpenho('EMPENHADO')).toBe('Empenhado')
    expect(formatarStatusEmpenho('LIQUIDADO')).toBe('Liquidado')
    expect(formatarStatusEmpenho('PAGO')).toBe('Pago')
    expect(formatarStatusEmpenho('ANULADO')).toBe('Anulado')
  })

  it('mantém o valor original para status desconhecidos', () => {
    expect(formatarStatusEmpenho('CANCELADO')).toBe('CANCELADO')
  })
})

describe('extrairMensagemErro', () => {
  it('prioriza a lista de detalhes', () => {
    const erro = { response: { data: { mensagem: 'Requisição inválida', detalhes: ['a', 'b', 'c'] } } }
    expect(extrairMensagemErro(erro)).toBe('a · b · c')
  })

  it('usa a mensagem quando não há detalhes', () => {
    const erro = { response: { data: { mensagem: 'Dotação não encontrada.', detalhes: [] } } }
    expect(extrairMensagemErro(erro)).toBe('Dotação não encontrada.')
  })

  it('usa apenas a mensagem quando detalhes é ausente', () => {
    const erro = { response: { data: { mensagem: 'Resposta sem detalhes desses' } } }
    expect(extrairMensagemErro(erro)).toBe('Resposta sem detalhes desses')
  })

  it('retorna mensagem padrão para erros sem resposta estruturada', () => {
    expect(extrairMensagemErro(new Error('sem response'))).toBe('Erro inesperado. Tente novamente.')
    expect(extrairMensagemErro(undefined)).toBe('Erro inesperado. Tente novamente.')
  })
})