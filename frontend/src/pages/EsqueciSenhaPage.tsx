import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api'
import CampoSenha from '../components/CampoSenha'
import { confirmaSenhaValida, senhaValidaMinima } from '../utils/validacao'
import { extrairMensagemErro } from '../utils/format'

export default function EsqueciSenhaPage() {
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [codigo, setCodigo] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [codigoEnviado, setCodigoEnviado] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [aguardando, setAguardando] = useState(false)

  async function solicitarCodigo(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando(true)
    try {
      await api.post('/auth/esqueci-senha', { email })
      setCodigoEnviado(true)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(false)
    }
  }

  async function redefinir(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    if (!senhaValidaMinima(novaSenha)) {
      setErro('A nova senha deve ter no mínimo 8 caracteres.')
      return
    }
    if (!confirmaSenhaValida(novaSenha, confirmacao)) {
      setErro('A confirmação não confere com a nova senha.')
      return
    }
    setAguardando(true)
    try {
      await api.post('/auth/redefinir-senha', { email, codigo, novaSenha })
      navegar('/login', { state: { senhaRedefinida: true } })
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(false)
    }
  }

  return (
    <div className="tela-auxiliar">
      <div className="card card-login">
        <h1>Recuperar senha</h1>

        {!codigoEnviado ? (
          <form onSubmit={solicitarCodigo}>
            <p className="subtitulo">
              Informe o seu e-mail para receber um código de recuperação de 6 dígitos
              (válido por 15 minutos).
            </p>

            <label htmlFor="email-recuperacao">E-mail</label>
            <input
              id="email-recuperacao"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="voce@prefeitura.gov.br"
              autoComplete="email"
              required
            />

            {erro && <div className="alerta erro" role="alert">{erro}</div>}

            <button className="btn primario" type="submit" disabled={aguardando}>
              {aguardando ? 'Enviando…' : 'Enviar código'}
            </button>
          </form>
        ) : (
          <form onSubmit={redefinir}>
            <p className="subtitulo">
              Um código foi enviado para <strong>{email}</strong>.
              Se não encontrar na caixa de entrada, confira o lixo eletrônico.
            </p>

            <label htmlFor="codigo-recuperacao">Código</label>
            <input
              id="codigo-recuperacao"
              type="text"
              inputMode="numeric"
              pattern="[0-9]{6}"
              maxLength={6}
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))}
              placeholder="000000"
              required
            />

            <CampoSenha
              id="nova-senha-recuperacao"
              label="Nova senha"
              value={novaSenha}
              onChange={setNovaSenha}
              placeholder="Mínimo de 8 caracteres"
              autoComplete="new-password"
              minLength={8}
              required
            />

            <CampoSenha
              id="confirmar-nova-senha-recuperacao"
              label="Confirmar nova senha"
              value={confirmacao}
              onChange={setConfirmacao}
              placeholder="Repita a nova senha"
              autoComplete="new-password"
              minLength={8}
              required
            />

            {erro && <div className="alerta erro" role="alert">{erro}</div>}

            <button className="btn primario" type="submit" disabled={aguardando}>
              {aguardando ? 'Redefinindo…' : 'Redefinir senha'}
            </button>
          </form>
        )}

        <p className="link-alternativo">
          <Link to="/login">Voltar para o login</Link>
        </p>
      </div>
    </div>
  )
}