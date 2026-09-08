import { createContext } from 'react'
import type { OrganizacaoInfo } from '../services/organizacoes'

export interface UsuarioLogado {
  id: number
  nome: string
  email: string
  perfil: string
  organizacoes: OrganizacaoInfo[]
}

export interface AuthContexto {
  usuario: UsuarioLogado | null
  autenticado: boolean
  ehAdmin: boolean
  login: (email: string, senha: string) => Promise<UsuarioLogado>
  logout: () => void
  recarregarOrganizacoes: () => Promise<UsuarioLogado>
}

export const AuthContext = createContext<AuthContexto | null>(null)