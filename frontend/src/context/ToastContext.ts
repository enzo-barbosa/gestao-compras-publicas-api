import { createContext } from 'react'

export type TipoToast = 'sucesso' | 'erro' | 'aviso'

export interface ContextoToasts {
  exibir: (tipo: TipoToast, mensagem: string) => void
}

export const ToastContext = createContext<ContextoToasts | null>(null)