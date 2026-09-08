import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/useAuth'
import { organizacaoAtiva, orgIdAtiva } from '../services/organizacoes'

export default function GuardiaOrganizacao({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  if (!usuario) {
    return <Navigate to="/login" replace />
  }
  if (usuario.organizacoes.length === 0) {
    return <Navigate to="/onboarding" replace />
  }
  if (!organizacaoAtiva(usuario.organizacoes, orgIdAtiva())) {
    return <Navigate to="/selecionar-grupo" replace />
  }
  return <>{children}</>
}