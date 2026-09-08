import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import { definirOrgAtiva } from '../services/organizacoes'
import { extrairMensagemErro } from '../utils/format'

export default function OnboardingPage() {
  const { usuario, recarregarOrganizacoes } = useAuth()
  const { exibir } = useToast()
  const navegar = useNavigate()
  const [nomeGrupo, setNomeGrupo] = useState('')
  const [codigo, setCodigo] = useState('')
  const [aguardando, setAguardando] = useState<'grupo' | 'codigo' | 'email' | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  async function criarGrupo(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAguardando('grupo')
    try {
      const resposta = await api.post<{ id: number }>('/organizacoes', { nome: nomeGrupo })
      const atualizado = await recarregarOrganizacoes()
      definirOrgAtiva(resposta.data.id)
      exibir('sucesso', `Grupo "${nomeGrupo}" criado.`)
      void atualizado
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

  async function averiguarConvites() {
    setErro(null)
    setAguardando('email')
    try {
      const resposta = await api.post<{ id: number }[]>('/convites/aceitar-email')
      if (resposta.data.length === 0) {
        exibir('aviso', 'Nenhum convite pendente no seu e-mail por enquanto.')
        return
      }
      const atualizado = await recarregarOrganizacoes()
      definirOrgAtiva(resposta.data[0]?.id ?? null)
      void atualizado
      exibir('sucesso', `${resposta.data.length} convite(s) aceito(s).`)
      navegar('/app')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAguardando(null)
    }
  }

  return (
    <div className="tela-auxiliar">
      <div className="card card-onboarding">
        <h1>Bem-vindo, {usuario?.nome}!</h1>
        <p className="subtitulo">
          Você ainda não participa de nenhum grupo. Crie um ou aceite um convite para começar.
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
            <p>Recebeu um código de um grupo? Informe-o para entrar na organização.</p>
            <label htmlFor="codigo">Código do convite</label>
            <input
              id="codigo"
              type="text"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value)}
              placeholder="Ex.: ACESSO-2026"
              maxLength={24}
              required
            />
            <button className="btn primario" type="submit" disabled={aguardando === 'codigo'}>
              {aguardando === 'codigo' ? 'Aceitando…' : 'Aceitar convite'}
            </button>
          </form>
        </div>

        <div className="linha-aviso">
          <span>Alguém te convidou por e-mail?</span>
          <button
            className="btn secundario"
            type="button"
            onClick={averiguarConvites}
            disabled={aguardando === 'email'}
          >
            {aguardando === 'email' ? 'Verificando…' : 'Verificar convites'}
          </button>
        </div>
      </div>
    </div>
  )
}