import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import CampoSenha from './CampoSenha'
import { confirmaSenhaValida, senhaValidaMinima } from '../utils/validacao'

interface Props {
  nomeMembro: string
  salvando: boolean
  erro: string | null
  aoSalvar: (novaSenha: string) => void
  aoCancelar: () => void
}

export default function ModalRedefinirSenha({
  nomeMembro,
  salvando,
  erro,
  aoSalvar,
  aoCancelar,
}: Props) {
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erroValidacao, setErroValidacao] = useState<string | null>(null)
  const dialogo = useRef<HTMLDivElement>(null)

  useEffect(() => {
    dialogo.current?.focus()

    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') aoCancelar()
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [aoCancelar])

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!senhaValidaMinima(novaSenha)) {
      setErroValidacao('A nova senha deve ter no mínimo 8 caracteres.')
      return
    }
    if (!confirmaSenhaValida(novaSenha, confirmacao)) {
      setErroValidacao('A confirmação não confere com a nova senha.')
      return
    }
    setErroValidacao(null)
    aoSalvar(novaSenha)
  }

  const mensagemErro = erroValidacao ?? erro

  return (
    <div className="modal-overlay" onMouseDown={aoCancelar}>
      <div
        ref={dialogo}
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-senha-titulo"
        tabIndex={-1}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h3 id="modal-senha-titulo">Redefinir senha de {nomeMembro}</h3>
        <p className="dica">
          O membro será desconectado de todas as sessões e entrará com a nova senha.
        </p>
        <form className="grade-form" onSubmit={enviar}>
          <CampoSenha
            id="membro-nova-senha"
            label="Nova senha"
            value={novaSenha}
            onChange={setNovaSenha}
            placeholder="Mínimo de 8 caracteres"
            autoComplete="new-password"
            minLength={8}
            required
          />
          <CampoSenha
            id="membro-confirmar-senha"
            label="Confirmar nova senha"
            value={confirmacao}
            onChange={setConfirmacao}
            placeholder="Repita a nova senha"
            autoComplete="new-password"
            minLength={8}
            required
          />
          {mensagemErro && (
            <div className="alerta erro" role="alert">{mensagemErro}</div>
          )}
          <div className="modal-acoes">
            <button
              type="button"
              className="btn secundario"
              onClick={aoCancelar}
              disabled={salvando}
            >
              Cancelar
            </button>
            <button type="submit" className="btn primario" disabled={salvando}>
              {salvando ? 'Redefinindo…' : 'Redefinir senha'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
