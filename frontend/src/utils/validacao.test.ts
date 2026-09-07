import { describe, expect, it } from 'vitest'
import { apenasDigitos, cnpjValido, dataEncerramentoValida } from './validacao'

describe('apenasDigitos', () => {
  it('remove pontos, barras e hifens do CNPJ', () => {
    expect(apenasDigitos('12.345.678/0001-95')).toBe('12345678000195')
  })

  it('remove letras e espaços', () => {
    expect(apenasDigitos('abc 12 34')).toBe('1234')
  })

  it('mantém string sem caracteres especiais', () => {
    expect(apenasDigitos('12345678901234')).toBe('12345678901234')
  })
})

describe('cnpjValido', () => {
  it('reconhece CNPJ com máscara', () => {
    expect(cnpjValido('12.345.678/0001-95')).toBe(true)
  })

  it('reconhece CNPJ apenas numérico', () => {
    expect(cnpjValido('12345678901234')).toBe(true)
  })

  it('rejeita CNPJ com quantidade incorreta de dígitos', () => {
    expect(cnpjValido('1234')).toBe(false)
    expect(cnpjValido('')).toBe(false)
  })
})

describe('dataEncerramentoValida', () => {
  it('aceita encerramento após a abertura', () => {
    expect(dataEncerramentoValida('2026-01-10', '2026-02-01')).toBe(true)
  })

  it('aceita encerramento na mesma data', () => {
    expect(dataEncerramentoValida('2026-01-10', '2026-01-10')).toBe(true)
  })

  it('rejeita encerramento anterior à abertura', () => {
    expect(dataEncerramentoValida('2026-02-01', '2026-01-10')).toBe(false)
  })

  it('aceita licitação sem data de encerramento', () => {
    expect(dataEncerramentoValida('2026-01-10', null)).toBe(true)
    expect(dataEncerramentoValida('2026-01-10', '')).toBe(true)
  })
})