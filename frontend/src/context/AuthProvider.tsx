import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import api, { TOKEN_KEY, USUARIO_KEY, ORGAO_KEY } from '../services/api'
import { EVENTO_ORG, orgIdAtiva, papelAtivo, podeGerir } from '../services/organizacoes'
import { AuthContext } from './AuthContext'
import type { UsuarioLogado } from './AuthContext'

function carregarUsuario(): UsuarioLogado | null {
  try {
    const bruto = localStorage.getItem(USUARIO_KEY)
    if (!bruto) return null
    const dados = JSON.parse(bruto) as Partial<UsuarioLogado>
    if (!dados.id || !dados.nome) return null
    return {
      id: dados.id,
      nome: dados.nome,
      email: dados.email ?? '',
      perfil: dados.perfil ?? 'USUARIO',
      organizacoes: dados.organizacoes ?? [],
    }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(carregarUsuario)
  const [orgPapel, setOrgPapel] = useState<string | undefined>(() =>
    papelAtivo(usuario?.organizacoes ?? [], orgIdAtiva()),
  )

  useEffect(() => {
    const atualizar = () => {
      setOrgPapel(papelAtivo(usuario?.organizacoes ?? [], orgIdAtiva()))
    }
    window.addEventListener(EVENTO_ORG, atualizar)
    window.addEventListener('storage', atualizar)
    return () => {
      window.removeEventListener(EVENTO_ORG, atualizar)
      window.removeEventListener('storage', atualizar)
    }
  }, [usuario])

  const buscarUsuario = useCallback(async (): Promise<UsuarioLogado> => {
    const dados = await api.get<UsuarioLogado>('/auth/me').then((r) => r.data)
    const normalizado: UsuarioLogado = {
      id: dados.id,
      nome: dados.nome,
      email: dados.email,
      perfil: dados.perfil,
      organizacoes: dados.organizacoes ?? [],
    }
    localStorage.setItem(USUARIO_KEY, JSON.stringify(normalizado))
    return normalizado
  }, [])

  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return
    buscarUsuario()
      .then(setUsuario)
      .catch(() => {})
  }, [buscarUsuario])

  const login = useCallback(
    async (email: string, senha: string): Promise<UsuarioLogado> => {
      const resposta = await api.post('/auth/login', { email, senha })
      const dados = resposta.data as { token: string }
      localStorage.setItem(TOKEN_KEY, dados.token)
      try {
        const logado = await buscarUsuario()
        setUsuario(logado)
        return logado
      } catch (e) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USUARIO_KEY)
        throw e
      }
    },
    [buscarUsuario],
  )

  const recarregarOrganizacoes = useCallback(async (): Promise<UsuarioLogado> => {
    const atualizado = await buscarUsuario()
    setUsuario(atualizado)
    return atualizado
  }, [buscarUsuario])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USUARIO_KEY)
    localStorage.removeItem(ORGAO_KEY)
    setUsuario(null)
  }, [])

  const valor = useMemo(
    () => ({
      usuario,
      autenticado: !!usuario,
      ehAdmin: podeGerir(usuario?.perfil ?? '', orgPapel),
      login,
      logout,
      recarregarOrganizacoes,
    }),
    [usuario, orgPapel, login, logout, recarregarOrganizacoes],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}