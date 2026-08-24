import { useContext } from 'react'
import { AuthContext } from './AuthContext'
import type { AuthContexto } from './AuthContext'

export function useAuth(): AuthContexto {
  const contexto = useContext(AuthContext)
  if (!contexto) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider')
  }
  return contexto
}
