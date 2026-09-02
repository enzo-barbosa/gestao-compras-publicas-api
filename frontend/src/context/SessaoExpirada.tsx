import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { AUTH_EXPIRADO } from '../services/api'
import { useAuth } from './useAuth'

export function SessaoExpiradaListener() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    const aoExpirar = () => {
      logout()
      navigate('/login', { replace: true })
    }
    window.addEventListener(AUTH_EXPIRADO, aoExpirar)
    return () => window.removeEventListener(AUTH_EXPIRADO, aoExpirar)
  }, [logout, navigate])

  return null
}
