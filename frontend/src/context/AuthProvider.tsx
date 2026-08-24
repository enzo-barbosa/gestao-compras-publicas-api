import { useCallback, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import api, { TOKEN_KEY, USUARIO_KEY } from '../services/api'
import { AuthContext } from './AuthContext'
import type { UsuarioLogado } from './AuthContext'

function carregarUsuario(): UsuarioLogado | null {
  try {
    const bruto = localStorage.getItem(USUARIO_KEY)
    return bruto ? (JSON.parse(bruto) as UsuarioLogado) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(carregarUsuario)

  const login = useCallback(async (email: string, senha: string) => {
    const resposta = await api.post('/auth/login', { email, senha })
    const dados = resposta.data as {
      token: string
      usuarioId: number
      nome: string
      email: string
      perfil: string
    }
    localStorage.setItem(TOKEN_KEY, dados.token)
    const logado: UsuarioLogado = {
      id: dados.usuarioId,
      nome: dados.nome,
      email: dados.email,
      perfil: dados.perfil,
    }
    localStorage.setItem(USUARIO_KEY, JSON.stringify(logado))
    setUsuario(logado)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USUARIO_KEY)
    setUsuario(null)
  }, [])

  const valor = useMemo(
    () => ({
      usuario,
      autenticado: !!usuario,
      ehAdmin: usuario?.perfil === 'ADMIN',
      login,
      logout,
    }),
    [usuario, login, logout],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
