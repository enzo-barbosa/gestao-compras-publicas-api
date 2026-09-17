import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/useAuth'

export default function RotaProtegida({ children }: { children: ReactNode }) {
  const { autenticado, validando } = useAuth()
  if (validando) {
    return <p className="vazio">Carregando…</p>
  }
  if (!autenticado) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}