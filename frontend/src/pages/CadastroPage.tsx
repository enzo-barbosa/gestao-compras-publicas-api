import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api'
import { extrairMensagemErro } from '../utils/format'

export default function CadastroPage() {
  const navegar = useNavigate()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aguardando, setAguardando] = useState(false)

  async function submeter(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando(true)
    try {
      await api.post('/auth/register', { nome, email, senha })
      navegar('/login', { state: { cadastrado: true } })
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(false)
    }
  }

  return (
    <div className="tela-auxiliar">
      <form className="card card-login" onSubmit={submeter}>
        <h1>Criar conta</h1>
        <p className="subtitulo">Cadastro aberto — você entra e cria ou é convidado para um grupo.</p>

        <label htmlFor="nome">Nome</label>
        <input
          id="nome"
          type="text"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          placeholder="Seu nome completo"
          autoComplete="name"
          required
        />

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
          placeholder="Mínimo de 8 caracteres"
          autoComplete="new-password"
          minLength={8}
          required
        />

        {erro && <div className="alerta erro" role="alert">{erro}</div>}

        <button className="btn primario" type="submit" disabled={aguardando}>
          {aguardando ? 'Criando…' : 'Criar conta'}
        </button>

        <p className="link-alternativo">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  )
}