import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'

interface Props {
  aberto: boolean
  titulo: string
  mensagem: ReactNode
  rotuloConfirmar: string
  rotuloCancelar: string
  confirmando?: boolean
  aoConfirmar: () => void
  aoCancelar: () => void
}

export default function ModalConfirmacao({
  aberto,
  titulo,
  mensagem,
  rotuloConfirmar,
  rotuloCancelar,
  confirmando = false,
  aoConfirmar,
  aoCancelar,
}: Props) {
  const botaoConfirmar = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!aberto) return
    botaoConfirmar.current?.focus()

    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') aoCancelar()
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [aberto, aoCancelar])

  if (!aberto) return null

  return (
    <div className="modal-overlay" onMouseDown={aoCancelar}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-confirmacao-titulo"
        aria-describedby="modal-confirmacao-mensagem"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h3 id="modal-confirmacao-titulo">{titulo}</h3>
        <p id="modal-confirmacao-mensagem">{mensagem}</p>
        <div className="modal-acoes">
          <button type="button" className="btn secundario" onClick={aoCancelar} disabled={confirmando}>
            {rotuloCancelar}
          </button>
          <button ref={botaoConfirmar} type="button" className="btn perigo" onClick={aoConfirmar} disabled={confirmando}>
            {rotuloConfirmar}
          </button>
        </div>
      </div>
    </div>
  )
}