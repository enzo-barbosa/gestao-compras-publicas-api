import { useContext } from 'react'
import { ToastContext } from './ToastContext'
import type { ContextoToasts } from './ToastContext'

export function useToast(): ContextoToasts {
  const contexto = useContext(ToastContext)
  if (!contexto) {
    throw new Error('useToast deve ser usado dentro de ToastProvider.')
  }
  return contexto
}