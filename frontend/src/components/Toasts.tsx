import { useCallback, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ToastContext } from '../context/ToastContext'
import type { ContextoToasts, TipoToast } from '../context/ToastContext'

interface Toast {
  id: number
  tipo: TipoToast
  mensagem: string
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const proximoId = useRef(0)

  const exibir = useCallback((tipo: TipoToast, mensagem: string) => {
    proximoId.current += 1
    const id = proximoId.current
    setToasts((atuais) => [...atuais, { id, tipo, mensagem }])
    window.setTimeout(() => {
      setToasts((atuais) => atuais.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  const valor: ContextoToasts = useMemo(() => ({ exibir }), [exibir])

  return (
    <ToastContext.Provider value={valor}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.tipo}`} role="status">
            {t.mensagem}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}