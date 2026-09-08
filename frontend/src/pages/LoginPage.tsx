import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { extrairMensagemErro } from '../utils/format'

export default function LoginPage() {
  const { login } = useAuth()
  const navegar = useNavigate()
  const localizacao = useLocation()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aguardando, setAguardando] = useState(false)
  const [sessaoExpirada, setSessaoExpirada] = useState(() => {
    const estado = localizacao.state as { sessaoExpirada?: boolean; cadastrado?: boolean } | null
    return Boolean(estado?.sessaoExpirada)
  })
  const [cadastrado, setCadastrado] = useState(() => {
    const estado = localizacao.state as { cadastrado?: boolean } | null
    return Boolean(estado?.cadastrado)
  })

  useEffect(() => {
    if (sessaoExpirada) {
      navegar('/login', { replace: true })
    }
  }, [sessaoExpirada, navegar])

  async function submeter(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando(true)
    try {
      await login(email, senha)
      navegar('/app')
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setCadastrado(false)
      if (sessaoExpirada) setSessaoExpirada(false)
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

        {sessaoExpirada && (
          <div className="alerta aviso" role="alert">
            Sua sessão expirou. Entre novamente para continuar.
          </div>
        )}

        {cadastrado && (
          <div className="alerta sucesso" role="alert">
            Conta criada com sucesso. Faça login para continuar.
          </div>
        )}

        {erro && <div className="alerta erro" role="alert">{erro}</div>}

        <button className="btn primario" type="submit" disabled={aguardando}>
          {aguardando ? 'Entrando…' : 'Entrar'}
        </button>

        <p className="link-alternativo">
          Ainda não tem conta? <Link to="/cadastro">Criar conta</Link>
        </p>
      </form>
    </div>
  )
}
