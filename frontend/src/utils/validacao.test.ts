import { describe, expect, it } from 'vitest'
import {
  apenasDigitos,
  cnpjValido,
  confirmaSenhaValida,
  dataEncerramentoValida,
  senhaValidaMinima,
} from './validacao'

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

describe('senhaValidaMinima', () => {
  it('aceita senha com 8 ou mais caracteres', () => {
    expect(senhaValidaMinima('12345678')).toBe(true)
    expect(senhaValidaMinima('umasenhaBemLonga')).toBe(true)
  })

  it('rejeita senha menor que 8 caracteres', () => {
    expect(senhaValidaMinima('1234567')).toBe(false)
    expect(senhaValidaMinima('')).toBe(false)
  })
})

describe('confirmaSenhaValida', () => {
  it('aceita quando senha e confirmação coincidem', () => {
    expect(confirmaSenhaValida('segredo123', 'segredo123')).toBe(true)
  })

  it('rejeita quando as senhas não coincidem', () => {
    expect(confirmaSenhaValida('segredo123', 'segredo124')).toBe(false)
  })

  it('rejeita confirmação vazia', () => {
    expect(confirmaSenhaValida('segredo123', '')).toBe(false)
  })
})