import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { TOKEN_KEY } from '../services/api'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import CampoSenha from '../components/CampoSenha'
import { confirmaSenhaValida, senhaValidaMinima } from '../utils/validacao'
import { extrairMensagemErro } from '../utils/format'

export default function MinhaContaPage() {
  const { usuario, recarregarOrganizacoes, logout } = useAuth()
  const { exibir } = useToast()
  const navegar = useNavigate()

  const [nome, setNome] = useState(usuario?.nome ?? '')
  const [salvandoNome, setSalvandoNome] = useState(false)
  const [erroNome, setErroNome] = useState<string | null>(null)

  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [alterandoSenha, setAlterandoSenha] = useState(false)
  const [erroSenha, setErroSenha] = useState<string | null>(null)

  const [confirmandoSaida, setConfirmandoSaida] = useState(false)
  const [saindo, setSaindo] = useState(false)
  const [erroSaida, setErroSaida] = useState<string | null>(null)

  async function salvarNome(evento: FormEvent) {
    evento.preventDefault()
    setErroNome(null)
    if (!nome.trim()) {
      setErroNome('Informe um nome.')
      return
    }
    setSalvandoNome(true)
    try {
      await api.put('/auth/minha-conta', { nome: nome.trim() })
      await recarregarOrganizacoes()
      exibir('sucesso', 'Dados atualizados.')
    } catch (e) {
      setErroNome(extrairMensagemErro(e))
    } finally {
      setSalvandoNome(false)
    }
  }

  async function alterarSenha(evento: FormEvent) {
    evento.preventDefault()
    setErroSenha(null)
    if (!senhaValidaMinima(novaSenha)) {
      setErroSenha('A nova senha deve ter no mínimo 8 caracteres.')
      return
    }
    if (!confirmaSenhaValida(novaSenha, confirmacao)) {
      setErroSenha('A confirmação não confere com a nova senha.')
      return
    }
    setAlterandoSenha(true)
    try {
      const resposta = await api.put<{ token: string }>('/auth/alterar-senha', {
        senhaAtual,
        novaSenha,
      })
      localStorage.setItem(TOKEN_KEY, resposta.data.token)
      setSenhaAtual('')
      setNovaSenha('')
      setConfirmacao('')
      exibir('sucesso', 'Senha alterada. As demais sessões foram encerradas.')
    } catch (e) {
      setErroSenha(extrairMensagemErro(e))
    } finally {
      setAlterandoSenha(false)
    }
  }

  async function sairEmTodosDispositivos() {
    if (!confirmandoSaida) {
      setConfirmandoSaida(true)
      return
    }
    setErroSaida(null)
    setSaindo(true)
    try {
      await api.post('/auth/logout-todos')
      logout()
      navegar('/login', { state: { sessaoEncerrada: true } })
    } catch (e) {
      setErroSaida(extrairMensagemErro(e))
      setConfirmandoSaida(false)
    } finally {
      setSaindo(false)
    }
  }

  return (
    <section>
      <h2>Minha conta</h2>

      <div className="card form-card">
        <h3>Dados pessoais</h3>
        <form className="grade-form" onSubmit={salvarNome}>
          <div>
            <label htmlFor="nome-conta">Nome</label>
            <input
              id="nome-conta"
              type="text"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              placeholder="Seu nome completo"
              autoComplete="name"
              required
            />
          </div>
          <div>
            <label htmlFor="email-conta">E-mail</label>
            <input id="email-conta" type="email" value={usuario?.email ?? ''} disabled />
          </div>
          <div className="acoes-form">
            <button className="btn primario" type="submit" disabled={salvandoNome}>
              {salvandoNome ? 'Salvando…' : 'Salvar dados'}
            </button>
          </div>
        </form>
        {erroNome && <div className="alerta erro" role="alert">{erroNome}</div>}
      </div>

      <div className="card form-card destaque">
        <h3>Alterar senha</h3>
        <p className="dica">
          Ao trocar a senha, o acesso nas outras sessões é encerrado automaticamente.
        </p>
        <form className="grade-form" onSubmit={alterarSenha}>
          <CampoSenha
            id="senha-atual"
            label="Senha atual"
            value={senhaAtual}
            onChange={setSenhaAtual}
            placeholder="••••••••"
            autoComplete="current-password"
            required
          />
          <CampoSenha
            id="nova-senha"
            label="Nova senha"
            value={novaSenha}
            onChange={setNovaSenha}
            placeholder="Mínimo de 8 caracteres"
            autoComplete="new-password"
            minLength={8}
            required
          />
          <CampoSenha
            id="confirmar-nova-senha"
            label="Confirmar nova senha"
            value={confirmacao}
            onChange={setConfirmacao}
            placeholder="Repita a nova senha"
            autoComplete="new-password"
            minLength={8}
            required
          />
          <div className="acoes-form">
            <button className="btn primario" type="submit" disabled={alterandoSenha}>
              {alterandoSenha ? 'Alterando…' : 'Alterar senha'}
            </button>
          </div>
        </form>
        {erroSenha && <div className="alerta erro" role="alert">{erroSenha}</div>}
      </div>

      <div className="card form-card">
        <h3>Sessões</h3>
        <p className="dica">
          Encerre o acesso em todos os navegadores e dispositivos conectados com a sua conta.
        </p>
        <button
          className={`btn perigo${confirmandoSaida ? ' confirmando' : ''}`}
          type="button"
          onClick={sairEmTodosDispositivos}
          disabled={saindo}
        >
          {saindo
            ? 'Encerrando…'
            : confirmandoSaida
              ? 'Clique de novo para confirmar'
              : 'Sair em todos os dispositivos'}
        </button>
        {erroSaida && <div className="alerta erro" role="alert">{erroSaida}</div>}
      </div>
    </section>
  )
}