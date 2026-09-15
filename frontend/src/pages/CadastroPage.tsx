import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api'
import CampoSenha from '../components/CampoSenha'
import TopoPublico from '../components/TopoPublico'
import PainelMarca from '../components/PainelMarca'
import { confirmaSenhaValida, senhaValidaMinima } from '../utils/validacao'
import { extrairMensagemErro } from '../utils/format'

export default function CadastroPage() {
  const navegar = useNavigate()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aguardando, setAguardando] = useState(false)
  const [emailJaUsado, setEmailJaUsado] = useState(false)

  async function submeter(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEmailJaUsado(false)
    if (!senhaValidaMinima(senha)) {
      setErro('A senha deve ter no mínimo 8 caracteres.')
      return
    }
    if (!confirmaSenhaValida(senha, confirmacao)) {
      setErro('A confirmação não confere com a senha informada.')
      return
    }
    setAguardando(true)
    try {
      await api.post('/auth/register', { nome, email, senha })
      navegar('/login', { state: { cadastrado: true } })
    } catch (e) {
      const http = e as { response?: { status?: number } }
      setEmailJaUsado(http.response?.status === 409)
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(false)
    }
  }

  return (
    <div className="tela-split">
      <TopoPublico />
      <div className="tela-split-corpo">
        <PainelMarca
          titulo="Crie sua conta e comece a organizar o orçamento hoje."
          bullets={[
            { titulo: 'Cadastro aberto e rápido', texto: 'Em menos de um minuto você já tem acesso ao sistema.' },
            { titulo: 'Do orçamento ao empenho', texto: 'Dotações, licitações, contratos e empenhos em um só lugar.' },
            { titulo: 'Colaboração segura', texto: 'Convide colegas por e-mail ou código e defina o papel de cada um.' },
          ]}
        />
        <main className="painel-form">
          <form className="card card-login" onSubmit={submeter}>
            <h1>Criar conta</h1>
            <p className="subtitulo">Use seu e-mail institucional do órgão.</p>

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

            <CampoSenha
              id="senha"
              label="Senha"
              value={senha}
              onChange={setSenha}
              placeholder="Mínimo de 8 caracteres"
              autoComplete="new-password"
              minLength={8}
              required
            />

            <CampoSenha
              id="confirmacao"
              label="Confirmar senha"
              value={confirmacao}
              onChange={setConfirmacao}
              placeholder="Repita a senha"
              autoComplete="new-password"
              minLength={8}
              required
            />

            {erro && <div className="alerta erro" role="alert">{erro}</div>}

            {emailJaUsado && (
              <p className="alerta aviso">
                Já tem uma conta? <Link to="/login">Entre aqui</Link>
              </p>
            )}

            <button className="btn primario" type="submit" disabled={aguardando}>
              {aguardando ? 'Criando…' : 'Criar conta'}
            </button>

            <p className="link-alternativo">
              Já tem conta? <Link to="/login">Entrar</Link>
            </p>
          </form>
        </main>
      </div>
    </div>
  )
}