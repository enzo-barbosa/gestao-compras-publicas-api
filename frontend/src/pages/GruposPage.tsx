import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import { definirOrgAtiva } from '../services/organizacoes'
import { extrairMensagemErro } from '../utils/format'

interface ConvitePendente {
  id: number
  organizacaoId: number
  organizacaoNome: string
  papel: string
  criadoEm: string
  expiraEm: string | null
}

export default function GruposPage() {
  const { recarregarOrganizacoes } = useAuth()
  const { exibir } = useToast()
  const navegar = useNavigate()
  const [nomeGrupo, setNomeGrupo] = useState('')
  const [codigo, setCodigo] = useState('')
  const [aguardando, setAguardando] = useState<'grupo' | 'codigo' | 'convites' | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [convitesPendentes, setConvitesPendentes] = useState<ConvitePendente[]>([])
  const [carregandoPendentes, setCarregandoPendentes] = useState(true)

  useEffect(() => {
    let ativo = true
    api.get<ConvitePendente[]>('/convites/pendentes')
      .then((r) => {
        if (ativo) setConvitesPendentes(r.data)
      })
      .catch(() => undefined)
      .finally(() => {
        if (ativo) setCarregandoPendentes(false)
      })
    return () => {
      ativo = false
    }
  }, [])

  async function criarGrupo(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando('grupo')
    try {
      const resposta = await api.post<{ id: number }>('/organizacoes', { nome: nomeGrupo })
      await recarregarOrganizacoes()
      definirOrgAtiva(resposta.data.id)
      exibir('sucesso', `Grupo "${nomeGrupo}" criado.`)
      navegar('/app')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(null)
    }
  }

  async function aceitarPorCodigo(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando('codigo')
    try {
      const resposta = await api.post<{ id: number }>('/convites/aceitar', { codigo })
      await recarregarOrganizacoes()
      definirOrgAtiva(resposta.data.id)
      exibir('sucesso', 'Convite aceito.')
      navegar('/app')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(null)
    }
  }

  async function aceitarConvite(convite: ConvitePendente) {
    setErro(null)
    setAguardando('convites')
    try {
      const resposta = await api.post<{ id: number }>(`/convites/${convite.id}/aceitar`)
      await recarregarOrganizacoes()
      definirOrgAtiva(resposta.data.id)
      exibir('sucesso', `Convite de "${convite.organizacaoNome}" aceito.`)
      navegar('/app')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(null)
    }
  }

  async function recusarConvite(convite: ConvitePendente) {
    setErro(null)
    setAguardando('convites')
    try {
      await api.post(`/convites/${convite.id}/recusar`)
      setConvitesPendentes((atuais) => atuais.filter((c) => c.id !== convite.id))
      exibir('aviso', `Convite de "${convite.organizacaoNome}" recusado.`)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(null)
    }
  }

  return (
    <section>
      <h2>Grupos</h2>
      <p className="dica">
        Todo novo usuário já nasce com seu espaço pessoal. Aqui você cria um grupo para a sua
        equipe, entra em um já existente pelo código de acesso ou aceita convites recebidos.
      </p>

      {erro && <div className="alerta erro" role="alert">{erro}</div>}

      <div className="grade-onboarding">
        <form className="opcao-onboarding" onSubmit={criarGrupo}>
          <h3>Criar um grupo</h3>
          <p>Monte a equipe do seu órgão e convide colegas depois. Você será o administrador.</p>
          <label htmlFor="nome-grupo">Nome do grupo</label>
          <input
            id="nome-grupo"
            type="text"
            value={nomeGrupo}
            onChange={(e) => setNomeGrupo(e.target.value)}
            placeholder="Ex.: Prefeitura de Exemplo"
            minLength={3}
            maxLength={120}
            required
          />
          <button className="btn primario" type="submit" disabled={aguardando === 'grupo'}>
            {aguardando === 'grupo' ? 'Criando…' : 'Criar grupo'}
          </button>
        </form>

        <form className="opcao-onboarding" onSubmit={aceitarPorCodigo}>
          <h3>Aceitar convite por código</h3>
          <p>O administrador do grupo informa o código de acesso de 8 caracteres.</p>
          <label htmlFor="codigo">Código de acesso</label>
          <input
            id="codigo"
            type="text"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value.toUpperCase().replace(/\s/g, ''))}
            placeholder="Ex.: F4K9XM2N"
            maxLength={8}
            required
          />
          <button className="btn primario" type="submit" disabled={aguardando === 'codigo'}>
            {aguardando === 'codigo' ? 'Aceitando…' : 'Aceitar convite'}
          </button>
        </form>
      </div>

      <div className="card form-card">
        <h3>Convites recebidos</h3>
        {carregandoPendentes ? (
          <p className="vazio">Carregando…</p>
        ) : convitesPendentes.length === 0 ? (
          <p className="vazio">Nenhum convite pendente no seu e-mail.</p>
        ) : (
          <ul className="lista-grupos">
            {convitesPendentes.map((convite) => (
              <li key={convite.id} className="opcao-onboarding">
                <div>
                  <strong>{convite.organizacaoNome}</strong>
                  <p>
                    Convite para participar como {convite.papel}. Aceite para entrar no grupo ou
                    recuse se não quiser participar.
                  </p>
                </div>
                <div className="acoes-linha">
                  <button
                    className="btn primario"
                    type="button"
                    disabled={aguardando === 'convites'}
                    onClick={() => aceitarConvite(convite)}
                  >
                    Aceitar
                  </button>
                  <button
                    className="btn secundario"
                    type="button"
                    disabled={aguardando === 'convites'}
                    onClick={() => recusarConvite(convite)}
                  >
                    Recusar
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}