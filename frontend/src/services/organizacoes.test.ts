import { describe, expect, it } from 'vitest'
import {
  destinoPosLogin,
  organizacaoAtiva,
  podeEmitirEmpenho,
  podeGerir,
} from './organizacoes'
import type { OrganizacaoInfo } from './organizacoes'

const orgs: OrganizacaoInfo[] = [
  { id: 1, nome: 'Prefeitura', papel: 'ADMIN' },
  { id: 2, nome: 'Câmara', papel: 'OPERADOR' },
]

describe('organizacaoAtiva', () => {
  it('retorna a organização quando o id existe na lista', () => {
    expect(organizacaoAtiva(orgs, 2)).toEqual(orgs[1])
  })

  it('retorna null para lista vazia', () => {
    expect(organizacaoAtiva([], 1)).toBeNull()
  })

  it('retorna null quando o id não pertence ao usuário', () => {
    expect(organizacaoAtiva(orgs, 999)).toBeNull()
  })

  it('retorna null sem id armazenado', () => {
    expect(organizacaoAtiva(orgs, null)).toBeNull()
  })
})

describe('destinoPosLogin', () => {
  it('vai ao onboarding sem nenhum grupo', () => {
    expect(destinoPosLogin([], 1)).toEqual({ rota: '/onboarding' })
  })

  it('vai ao app com grupo ativo válido', () => {
    expect(destinoPosLogin(orgs, 1)).toEqual({ rota: '/app' })
  })

  it('seleciona automaticamente o único grupo', () => {
    const um: OrganizacaoInfo[] = [orgs[0]!]
    expect(destinoPosLogin(um, null)).toEqual({ rota: '/app', orgAuto: 1 })
  })

  it('pede seleção com vários grupos sem grupo ativo', () => {
    expect(destinoPosLogin(orgs, null)).toEqual({ rota: '/selecionar-grupo' })
  })
})

describe('papéis por grupo', () => {
  it('SUPER_ADMIN gerencia e emite empenho', () => {
    expect(podeGerir('SUPER_ADMIN', undefined)).toBe(true)
    expect(podeEmitirEmpenho('SUPER_ADMIN', undefined)).toBe(true)
  })

  it('ADMIN na organização gerencia; OPERADOR não', () => {
    expect(podeGerir('USUARIO', 'ADMIN')).toBe(true)
    expect(podeGerir('USUARIO', 'OPERADOR')).toBe(false)
  })

  it('OPERADOR emite empenho; VISITANTE não', () => {
    expect(podeEmitirEmpenho('USUARIO', 'OPERADOR')).toBe(true)
    expect(podeEmitirEmpenho('USUARIO', 'VISITANTE')).toBe(false)
  })
})