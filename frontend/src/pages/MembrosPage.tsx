import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import { orgIdAtiva } from '../services/organizacoes'
import { useToast } from '../context/useToast'
import { extrairMensagemErro, formatarData } from '../utils/format'

interface Membro {
  usuarioId: number
  nome: string
  email: string
  papel: string
  desde: string
}

interface Convite {
  id: number
  email: string | null
  codigo: string | null
  papel: string
  criadoEm: string
}

const PAPEIS = ['ADMIN', 'OPERADOR', 'VISITANTE'] as const
type Aba = 'membros' | 'convites'
type TipoConvite = 'email' | 'codigo'

export default function MembrosPage() {
  const { exibir } = useToast()
  const organizacaoId = orgIdAtiva()
  const [aba, setAba] = useState<Aba>('membros')
  const [erro, setErro] = useState<string | null>(null)

  const [membros, setMembros] = useState<Membro[]>([])
  const [carregandoMembros, setCarregandoMembros] = useState(true)
  const [novoEmail, setNovoEmail] = useState('')
  const [novoPapel, setNovoPapel] = useState<string>(PAPEIS[0])
  const [adicionando, setAdicionando] = useState(false)

  const [convites, setConvites] = useState<Convite[]>([])
  const [carregandoConvites, setCarregandoConvites] = useState(false)
  const [tipoConvite, setTipoConvite] = useState<TipoConvite>('email')
  const [valorConvite, setValorConvite] = useState('')
  const [papelConvite, setPapelConvite] = useState<string>(PAPEIS[1])
  const [criandoConvite, setCriandoConvite] = useState(false)

  const buscarMembros = useCallback(async (): Promise<Membro[]> => {
    if (organizacaoId === null) return []
    const resposta = await api.get<Membro[]>(`/organizacoes/${organizacaoId}/membros`)
    return resposta.data
  }, [organizacaoId])

  const buscarConvites = useCallback(async (): Promise<Convite[]> => {
    if (organizacaoId === null) return []
    const resposta = await api.get<Convite[]>(`/organizacoes/${organizacaoId}/convites`)
    return resposta.data
  }, [organizacaoId])

  useEffect(() => {
    let ativo = true
    buscarMembros().then(
      (dados) => {
        if (ativo) {
          setMembros(dados)
          setErro(null)
        }
      },
      (e) => {
        if (ativo) setErro(extrairMensagemErro(e))
      },
    ).finally(() => {
      if (ativo) setCarregandoMembros(false)
    })
    return () => {
      ativo = false
    }
  }, [buscarMembros])

  function abrirConvites() {
    setAba('convites')
    setCarregandoConvites(true)
    buscarConvites().then(
      (dados) => {
        setConvites(dados)
        setErro(null)
      },
      (e) => setErro(extrairMensagemErro(e)),
    ).finally(() => setCarregandoConvites(false))
  }

  async function adicionarMembro(evento: FormEvent) {
    evento.preventDefault()
    if (organizacaoId === null) return
    setErro(null)
    setAdicionando(true)
    try {
      await api.post(`/organizacoes/${organizacaoId}/membros`, {
        email: novoEmail,
        papel: novoPapel,
      })
      exibir('sucesso', `Membro adicionado.`)
      setNovoEmail('')
      setMembros(await buscarMembros())
      setErro(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setAdicionando(false)
    }
  }

  async function alterarPapel(membro: Membro, papel: string) {
    if (organizacaoId === null || papel === membro.papel) return
    setErro(null)
    try {
      await api.put(`/organizacoes/${organizacaoId}/membros/${membro.usuarioId}`, { papel })
      exibir('sucesso', `${membro.nome} agora é ${papel}.`)
      setMembros((atuais) =>
        atuais.map((m) => (m.usuarioId === membro.usuarioId ? { ...m, papel } : m)),
      )
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function removerMembro(membro: Membro) {
    if (organizacaoId === null) return
    if (!window.confirm(`Remover ${membro.nome} deste grupo?`)) return
    setErro(null)
    try {
      await api.delete(`/organizacoes/${organizacaoId}/membros/${membro.usuarioId}`)
      exibir('sucesso', `${membro.nome} removido do grupo.`)
      setMembros((atuais) => atuais.filter((m) => m.usuarioId !== membro.usuarioId))
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function criarConvite(evento: FormEvent) {
    evento.preventDefault()
    if (organizacaoId === null) return
    setErro(null)
    setCriandoConvite(true)
    try {
      const corpo =
        tipoConvite === 'email'
          ? { email: valorConvite, papel: papelConvite }
          : { codigo: valorConvite, papel: papelConvite }
      await api.post(`/organizacoes/${organizacaoId}/convites`, corpo)
      exibir('sucesso', 'Convite criado.')
      setValorConvite('')
      setConvites(await buscarConvites())
      setErro(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setCriandoConvite(false)
    }
  }

  async function revogarConvite(convite: Convite) {
    if (organizacaoId === null) return
    if (!window.confirm('Revogar este convite?')) return
    setErro(null)
    try {
      await api.delete(`/organizacoes/${organizacaoId}/convites/${convite.id}`)
      exibir('sucesso', 'Convite revogado.')
      setConvites((atuais) => atuais.filter((c) => c.id !== convite.id))
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  function destinoConvite(convite: Convite): string {
    return convite.email ?? `Código ${convite.codigo}`
  }

  return (
    <section>
      <h2>Membros e convites</h2>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}
      <p className="dica">
        O criador do grupo não pode ser rebaixado nem removido. Convite por e-mail exige usuário
        cadastrado; o código pode ser compartilhado.
      </p>

      <div className="abas">
        <button
          type="button"
          className={`btn fantasma ${aba === 'membros' ? 'ativo' : ''}`}
          onClick={() => setAba('membros')}
        >
          Membros
        </button>
        <button
          type="button"
          className={`btn fantasma ${aba === 'convites' ? 'ativo' : ''}`}
          onClick={abrirConvites}
        >
          Convites
        </button>
      </div>

      {aba === 'membros' && (
        <>
          <form className="card form-card" onSubmit={adicionarMembro}>
            <h3>Adicionar membro</h3>
            <div className="grade-form">
              <div className="campo-largo">
                <label htmlFor="novo-email">E-mail do usuário</label>
                <input
                  id="novo-email"
                  type="email"
                  value={novoEmail}
                  onChange={(e) => setNovoEmail(e.target.value)}
                  placeholder="colega@prefeitura.gov.br"
                  required
                />
              </div>
              <div>
                <label htmlFor="novo-papel">Papel</label>
                <select
                  id="novo-papel"
                  value={novoPapel}
                  onChange={(e) => setNovoPapel(e.target.value)}
                >
                  {PAPEIS.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
              <button className="btn primario" type="submit" disabled={adicionando}>
                {adicionando ? 'Adicionando…' : 'Adicionar'}
              </button>
            </div>
          </form>

          {carregandoMembros ? (
            <p className="vazio">Carregando…</p>
          ) : membros.length === 0 ? (
            <p className="vazio">Nenhum membro além de você.</p>
          ) : (
            <div className="tabela-wrapper">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>E-mail</th>
                    <th>Papel</th>
                    <th>Desde</th>
                    <th aria-label="Ações">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {membros.map((membro) => (
                    <tr key={membro.usuarioId}>
                      <td>{membro.nome}</td>
                      <td>{membro.email}</td>
                      <td>
                        <select
                          value={membro.papel}
                          onChange={(e) => alterarPapel(membro, e.target.value)}
                          aria-label={`Papel de ${membro.nome}`}
                        >
                          {PAPEIS.map((p) => (
                            <option key={p} value={p}>{p}</option>
                          ))}
                        </select>
                      </td>
                      <td>{formatarData(membro.desde)}</td>
                      <td>
                        <button
                          className="btn perigo"
                          onClick={() => removerMembro(membro)}
                          aria-label={`Remover ${membro.nome}`}
                        >
                          Remover
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {aba === 'convites' && (
        <>
          <form className="card form-card" onSubmit={criarConvite}>
            <h3>Criar convite</h3>
            <div className="grade-form">
              <div className="barra-filtros">
                <button
                  type="button"
                  className={`btn fantasma ${tipoConvite === 'email' ? 'ativo' : ''}`}
                  onClick={() => setTipoConvite('email')}
                >
                  Por e-mail
                </button>
                <button
                  type="button"
                  className={`btn fantasma ${tipoConvite === 'codigo' ? 'ativo' : ''}`}
                  onClick={() => setTipoConvite('codigo')}
                >
                  Por código
                </button>
              </div>
              <div className="campo-largo">
                <label htmlFor="valor-convite">
                  {tipoConvite === 'email' ? 'E-mail do convidado' : 'Código do convite'}
                </label>
                <input
                  id="valor-convite"
                  type={tipoConvite === 'email' ? 'email' : 'text'}
                  value={valorConvite}
                  onChange={(e) => setValorConvite(e.target.value)}
                  placeholder={tipoConvite === 'email' ? 'colega@prefeitura.gov.br' : 'Ex.: ACESSO-2026'}
                  maxLength={tipoConvite === 'email' ? 150 : 24}
                  required
                />
              </div>
              <div>
                <label htmlFor="papel-convite">Papel</label>
                <select
                  id="papel-convite"
                  value={papelConvite}
                  onChange={(e) => setPapelConvite(e.target.value)}
                >
                  {PAPEIS.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
              <button className="btn primario" type="submit" disabled={criandoConvite}>
                {criandoConvite ? 'Criando…' : 'Criar convite'}
              </button>
            </div>
          </form>

          {carregandoConvites ? (
            <p className="vazio">Carregando…</p>
          ) : convites.length === 0 ? (
            <p className="vazio">Nenhum convite pendente.</p>
          ) : (
            <div className="tabela-wrapper">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Destino</th>
                    <th>Papel</th>
                    <th>Criado em</th>
                    <th aria-label="Ações">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {convites.map((convite) => (
                    <tr key={convite.id}>
                      <td className="mono">{destinoConvite(convite)}</td>
                      <td>{convite.papel}</td>
                      <td>{formatarData(convite.criadoEm)}</td>
                      <td>
                        <button
                          className="btn perigo"
                          onClick={() => revogarConvite(convite)}
                          aria-label={`Revogar convite ${destinoConvite(convite)}`}
                        >
                          Revogar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  )
}