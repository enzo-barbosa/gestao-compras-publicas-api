import { describe, expect, it } from 'vitest'
import {
  extrairMensagemErro,
  formatarCompetencia,
  formatarData,
  formatarMoeda,
  formatarStatusEmpenho,
  mascaraCnpj,
  mascaraMoeda,
  mascaraNumeroEdital,
  saudacaoBemVindo,
  valorDaMascaraMoeda,
} from './format'

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

  it('usa a mensagem de validações customizadas (Error sem response)', () => {
    expect(extrairMensagemErro(new Error('CNPJ inválido: verifique os números informados.')))
      .toBe('CNPJ inválido: verifique os números informados.')
    expect(extrairMensagemErro(new Error('Informe o número do contrato.')))
      .toBe('Informe o número do contrato.')
  })

  it('retorna mensagem padrão apenas para valores não estruturados', () => {
    expect(extrairMensagemErro(undefined)).toBe('Erro inesperado. Tente novamente.')
    expect(extrairMensagemErro(null)).toBe('Erro inesperado. Tente novamente.')
  })
})

describe('saudacaoBemVindo', () => {
  it('usa o nome informado', () => {
    expect(saudacaoBemVindo('Rita')).toBe('Bem-vindo(a), Rita!')
    expect(saudacaoBemVindo('João')).toBe('Bem-vindo(a), João!')
  })

  it('cobre nome ausente', () => {
    expect(saudacaoBemVindo(undefined)).toBe('Bem-vindo(a), você!')
  })
})

describe('mascaraMoeda', () => {
  it('agrupa milhares sem decimais', () => {
    expect(mascaraMoeda('25000')).toBe('25.000')
    expect(mascaraMoeda('1')).toBe('1')
    expect(mascaraMoeda('0')).toBe('0')
  })

  it('mantém centavos quando há separador decimal', () => {
    expect(mascaraMoeda('25000,50')).toBe('25.000,50')
    expect(mascaraMoeda('0,05')).toBe('0,05')
  })

  it('retorna vazio para valor vazio', () => {
    expect(mascaraMoeda('')).toBe('')
  })
})

describe('valorDaMascaraMoeda', () => {
  it('converte valor mascarado em número', () => {
    expect(valorDaMascaraMoeda('25.000,50')).toBe(25000.5)
    expect(valorDaMascaraMoeda('25.000')).toBe(25000)
    expect(valorDaMascaraMoeda('0,05')).toBe(0.05)
  })

  it('retorna zero para valor vazio', () => {
    expect(valorDaMascaraMoeda('')).toBe(0)
  })
})

describe('mascaraCnpj', () => {
  it('aplica a máscara completa', () => {
    expect(mascaraCnpj('12345678000195')).toBe('12.345.678/0001-95')
  })

  it('aplica a máscara gradualmente', () => {
    expect(mascaraCnpj('12')).toBe('12')
    expect(mascaraCnpj('123')).toBe('12.3')
    expect(mascaraCnpj('1234567')).toBe('12.345.67')
    expect(mascaraCnpj('123456789012')).toBe('12.345.678/9012')
  })

  it('retorna vazio para valor vazio', () => {
    expect(mascaraCnpj('')).toBe('')
  })
})

describe('mascaraNumeroEdital', () => {
  it('formata número/ano gradualmente', () => {
    expect(mascaraNumeroEdital('001')).toBe('001')
    expect(mascaraNumeroEdital('001/')).toBe('001/')
    expect(mascaraNumeroEdital('001/2026')).toBe('001/2026')
  })

  it('limita o tamanho de número e ano', () => {
    expect(mascaraNumeroEdital('12345/2026')).toBe('1234/2026')
    expect(mascaraNumeroEdital('001/20265')).toBe('001/2026')
  })

  it('remove caracteres inválidos e trunca número sem barra', () => {
    expect(mascaraNumeroEdital('001//2026')).toBe('001/2026')
    expect(mascaraNumeroEdital('001_2026')).toBe('0012')
  })
})