import { ORGAO_KEY } from './api'

export interface OrganizacaoInfo {
  id: number
  nome: string
  papel: string
}

export interface Destino {
  rota: '/onboarding' | '/selecionar-grupo' | '/app'
  orgAuto?: number
}

export function orgIdAtiva(): number | null {
  const bruto = localStorage.getItem(ORGAO_KEY)
  return bruto ? Number(bruto) : null
}

export const EVENTO_ORG = 'gc:org'

export function definirOrgAtiva(id: number | null): void {
  if (id === null) {
    localStorage.removeItem(ORGAO_KEY)
  } else {
    localStorage.setItem(ORGAO_KEY, String(id))
  }
  window.dispatchEvent(new Event(EVENTO_ORG))
}

export function organizacaoAtiva(
  organizacoes: OrganizacaoInfo[],
  id: number | null,
): OrganizacaoInfo | null {
  if (id === null || organizacoes.length === 0) return null
  return organizacoes.find((o) => o.id === id) ?? null
}

export function papelAtivo(
  organizacoes: OrganizacaoInfo[],
  id: number | null,
): string | undefined {
  return organizacaoAtiva(organizacoes, id)?.papel
}

export function destinoPosLogin(
  organizacoes: OrganizacaoInfo[],
  id: number | null,
): Destino {
  if (organizacoes.length === 0) return { rota: '/onboarding' }
  const ativa = organizacaoAtiva(organizacoes, id)
  if (ativa) return { rota: '/app' }
  if (organizacoes.length === 1) {
    return { rota: '/app', orgAuto: organizacoes[0]?.id }
  }
  return { rota: '/selecionar-grupo' }
}

export function podeGerir(
  perfil: string,
  papelOrg: string | undefined,
): boolean {
  return perfil === 'SUPER_ADMIN' || papelOrg === 'ADMIN'
}

export function podeEmitirEmpenho(
  perfil: string,
  papelOrg: string | undefined,
): boolean {
  return perfil === 'SUPER_ADMIN' || papelOrg === 'ADMIN' || papelOrg === 'OPERADOR'
}