import { createContext } from 'react'

export interface UsuarioLogado {
  id: number
  nome: string
  email: string
  perfil: string
}

export interface AuthContexto {
  usuario: UsuarioLogado | null
  autenticado: boolean
  ehAdmin: boolean
  login: (email: string, senha: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContexto | null>(null)
