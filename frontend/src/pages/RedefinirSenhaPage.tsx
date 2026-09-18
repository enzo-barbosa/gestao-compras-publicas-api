import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api'
import CampoSenha from '../components/CampoSenha'
import TopoPublico from '../components/TopoPublico'
import PainelMarca from '../components/PainelMarca'
import { confirmaSenhaValida, senhaValidaMinima } from '../utils/validacao'
import { extrairMensagemErro } from '../utils/format'

export default function RedefinirSenhaPage() {
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aguardando, setAguardando] = useState(false)

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
    if (novaSenha === senhaAtual) {
      setErro('A nova senha deve ser diferente da senha atual.')
      return
    }
    setAguardando(true)
    try {
      await api.post('/auth/redefinir-senha', { email, senhaAtual, novaSenha })
      navegar('/login', { state: { senhaRedefinida: true } })
    } catch (e) {
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
          titulo="Redefina sua senha com segurança."
          bullets={[
            { titulo: 'Confirme quem você é', texto: 'Informe o e-mail e a senha atual para liberar a troca.' },
            { titulo: 'Sessões encerradas', texto: 'Ao trocar a senha, todos os dispositivos são desconectados.' },
          ]}
        />
        <main className="painel-form">
          <div className="card card-login">
            <h1>Redefinir senha</h1>

            <form onSubmit={redefinir}>
              <p className="subtitulo">
                Informe o seu e-mail e a senha atual; em seguida escolha uma nova senha.
              </p>

              <label htmlFor="email-redefinicao">E-mail</label>
              <input
                id="email-redefinicao"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="voce@prefeitura.gov.br"
                autoComplete="email"
                required
              />

              <CampoSenha
                id="senha-atual-redefinicao"
                label="Senha atual"
                value={senhaAtual}
                onChange={setSenhaAtual}
                placeholder="Sua senha atual"
                autoComplete="current-password"
                required
              />

              <CampoSenha
                id="nova-senha-redefinicao"
                label="Nova senha"
                value={novaSenha}
                onChange={setNovaSenha}
                placeholder="Mínimo de 8 caracteres"
                autoComplete="new-password"
                minLength={8}
                required
              />

              <CampoSenha
                id="confirmar-nova-senha-redefinicao"
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

            <p className="link-alternativo">
              <Link to="/login">Voltar para o login</Link>
            </p>
          </div>
        </main>
      </div>
    </div>
  )
}
