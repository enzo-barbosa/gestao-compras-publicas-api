import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { extrairMensagemErro } from '../utils/format'

export default function LoginPage() {
  const { login } = useAuth()
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aguardando, setAguardando] = useState(false)

  async function submeter(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando(true)
    try {
      await login(email, senha)
      navegar('/')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(false)
    }
  }

  return (
    <div className="tela-login">
      <form className="card card-login" onSubmit={submeter}>
        <h1>Gestão de Compras Públicas</h1>
        <p className="subtitulo">Acesse com suas credenciais institucionais</p>

        <label htmlFor="email">E-mail</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="voce@prefeitura.gov.br"
          autoComplete="email"
          required
        />

        <label htmlFor="senha">Senha</label>
        <input
          id="senha"
          type="password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          placeholder="••••••••"
          autoComplete="current-password"
          required
        />

        {erro && <div className="alerta erro" role="alert">{erro}</div>}

        <button className="btn primario" type="submit" disabled={aguardando}>
          {aguardando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
