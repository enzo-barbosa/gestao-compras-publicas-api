import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'

export default function RotaProtegida({ children }: { children: ReactNode }) {
  const { autenticado } = useAuth()
  if (!autenticado) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}
