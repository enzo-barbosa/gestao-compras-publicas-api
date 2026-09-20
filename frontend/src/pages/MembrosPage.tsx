import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'
import { destinoPosLogin, definirOrgAtiva, orgIdAtiva, podeGerir } from '../services/organizacoes'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import ModalConfirmacao from '../components/ModalConfirmacao'
import ModalRedefinirSenha from '../components/ModalRedefinirSenha'
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
  expiraEm: string | null
}

const PAPEIS = ['ADMIN', 'OPERADOR', 'VISITANTE'] as const
type Aba = 'membros' | 'convites'

export default function MembrosPage() {
  const { exibir } = useToast()
  const { usuario, recarregarOrganizacoes } = useAuth()
  const navegar = useNavigate()
  const organizacaoId = orgIdAtiva()
  const papelOrg = usuario?.organizacoes.find((o) => o.id === organizacaoId)?.papel
  const gerencia = podeGerir(usuario?.perfil ?? '', papelOrg)
  const eAdminGrupo = papelOrg === 'ADMIN'

  const [aba, setAba] = useState<Aba>('membros')
  const [erro, setErro] = useState<string | null>(null)

  const [membros, setMembros] = useState<Membro[]>([])
  const [carregandoMembros, setCarregandoMembros] = useState(true)
  const [novoEmail, setNovoEmail] = useState('')
  const [novoPapel, setNovoPapel] = useState<string>(PAPEIS[0])
  const [adicionando, setAdicionando] = useState(false)

  const [convites, setConvites] = useState<Convite[]>([])
  const [carregandoConvites, setCarregandoConvites] = useState(false)
  const [emailConvite, setEmailConvite] = useState('')
  const [papelConvite, setPapelConvite] = useState<string>(PAPEIS[1])
  const [criandoConvite, setCriandoConvite] = useState(false)

  const [codigoAcesso, setCodigoAcesso] = useState<string | null>(null)
  const [gerandoCodigo, setGerandoCodigo] = useState(false)
  const [revogandoCodigo, setRevogandoCodigo] = useState(false)

  const [confirmandoSaida, setConfirmandoSaida] = useState(false)
  const [saindo, setSaindo] = useState(false)

  const [confirmandoExclusaoGrupo, setConfirmandoExclusaoGrupo] = useState(false)
  const [excluindoGrupo, setExcluindoGrupo] = useState(false)

  const [membroExclusao, setMembroExclusao] = useState<Membro | null>(null)
  const [conviteExclusao, setConviteExclusao] = useState<Convite | null>(null)
  const [removendo, setRemovendo] = useState(false)
  const [revogando, setRevogando] = useState(false)

  const [membroSenha, setMembroSenha] = useState<Membro | null>(null)
  const [redefinindoSenha, setRedefinindoSenha] = useState(false)
  const [erroSenha, setErroSenha] = useState<string | null>(null)

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

  useEffect(() => {
    if (organizacaoId === null || !gerencia) return
    api.get<{ codigo: string | null }>(`/organizacoes/${organizacaoId}/codigo-acesso`)
      .then((r) => setCodigoAcesso(r.data.codigo))
      .catch(() => undefined)
  }, [organizacaoId, gerencia])

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
      exibir('sucesso', 'Membro adicionado.')
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

  function removerMembro(membro: Membro) {
    setMembroExclusao(membro)
  }

  async function confirmarRemocao() {
    if (organizacaoId === null || membroExclusao === null) return
    const nomeRemovido = membroExclusao
    setErro(null)
    setRemovendo(true)
    try {
      await api.delete(`/organizacoes/${organizacaoId}/membros/${nomeRemovido.usuarioId}`)
      exibir('sucesso', `${nomeRemovido.nome} removido do grupo.`)
      setMembros((atuais) => atuais.filter((m) => m.usuarioId !== nomeRemovido.usuarioId))
      setMembroExclusao(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setMembroExclusao(null)
    } finally {
      setRemovendo(false)
    }
  }

  function redefinirSenhaDe(membro: Membro) {
    setErroSenha(null)
    setMembroSenha(membro)
  }

  async function confirmarRedefinicaoSenha(novaSenha: string) {
    if (organizacaoId === null || membroSenha === null) return
    const nomeAlvo = membroSenha.nome
    setErroSenha(null)
    setRedefinindoSenha(true)
    try {
      await api.put(`/organizacoes/${organizacaoId}/membros/${membroSenha.usuarioId}/senha`, {
        novaSenha,
      })
      exibir('sucesso', `Senha de ${nomeAlvo} redefinida. As sessões dele foram encerradas.`)
      setMembroSenha(null)
    } catch (e) {
      setErroSenha(extrairMensagemErro(e))
    } finally {
      setRedefinindoSenha(false)
    }
  }

  async function criarConvite(evento: FormEvent) {
    evento.preventDefault()
    if (organizacaoId === null) return
    setErro(null)
    setCriandoConvite(true)
    try {
      await api.post(`/organizacoes/${organizacaoId}/convites`, {
        email: emailConvite.trim(),
        papel: papelConvite,
      })
      exibir('sucesso', 'Convite criado. O convite expira em 7 dias.')
      setEmailConvite('')
      setConvites(await buscarConvites())
      setErro(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setCriandoConvite(false)
    }
  }

  function revogarConvite(convite: Convite) {
    setConviteExclusao(convite)
  }

  async function confirmarRevogacao() {
    if (organizacaoId === null || conviteExclusao === null) return
    const conviteRevogado = conviteExclusao
    setErro(null)
    setRevogando(true)
    try {
      await api.delete(`/organizacoes/${organizacaoId}/convites/${conviteRevogado.id}`)
      exibir('sucesso', 'Convite revogado.')
      setConvites((atuais) => atuais.filter((c) => c.id !== conviteRevogado.id))
      setConviteExclusao(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setConviteExclusao(null)
    } finally {
      setRevogando(false)
    }
  }

  function destinoConvite(convite: Convite): string {
    return convite.email ?? `Código ${convite.codigo}`
  }

  async function gerarCodigoAcesso() {
    if (organizacaoId === null) return
    setErro(null)
    setGerandoCodigo(true)
    try {
      const resposta = await api.post<{ codigo: string }>(`/organizacoes/${organizacaoId}/codigo-acesso`)
      setCodigoAcesso(resposta.data.codigo)
      exibir('sucesso', 'Código de acesso gerado: compartilhe com os novos integrantes.')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setGerandoCodigo(false)
    }
  }

  async function revogarCodigoAcesso() {
    if (organizacaoId === null) return
    setErro(null)
    setRevogandoCodigo(true)
    try {
      await api.delete(`/organizacoes/${organizacaoId}/codigo-acesso`)
      setCodigoAcesso(null)
      exibir('aviso', 'Código de acesso revogado; o código antigo deixou de funcionar.')
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setRevogandoCodigo(false)
    }
  }

  async function confirmarExclusaoGrupo() {
    if (organizacaoId === null) return
    setErro(null)
    setExcluindoGrupo(true)
    try {
      await api.delete(`/organizacoes/${organizacaoId}`)
      exibir('sucesso', 'Grupo excluído, incluindo todos os dados de negócio da organização.')
      setConfirmandoExclusaoGrupo(false)
      const atualizado = await recarregarOrganizacoes()
      const destino = destinoPosLogin(atualizado.organizacoes ?? [], null)
      definirOrgAtiva(destino.orgAuto ?? null)
      navegar(destino.rota)
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setConfirmandoExclusaoGrupo(false)
    } finally {
      setExcluindoGrupo(false)
    }
  }

  async function sairDoGrupo() {
    if (organizacaoId === null) return
    if (!confirmandoSaida) {
      setConfirmandoSaida(true)
      return
    }
    setErro(null)
    setSaindo(true)
    try {
      await api.delete(`/organizacoes/${organizacaoId}/membros/eu`)
      exibir('sucesso', 'Você saiu do grupo.')
      const atualizado = await recarregarOrganizacoes()
      const destino = destinoPosLogin(atualizado.organizacoes ?? [], null)
      navegar(destino.rota)
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setConfirmandoSaida(false)
    } finally {
      setSaindo(false)
    }
  }

  return (
    <section>
      <h2>Integrantes</h2>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}
      <p className="dica">
        Todos os integrantes veem esta lista. Papéis são ajustados pelo administrador do grupo.
      </p>

      {gerencia && (
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
      )}

      {aba === 'membros' && (
        <>
          {gerencia && (
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
          )}

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
                    {gerencia && <th aria-label="Ações">Ações</th>}
                  </tr>
                </thead>
                <tbody>
                  {membros.map((membro) => (
                    <tr key={membro.usuarioId}>
                      <td>
                        {membro.usuarioId === usuario?.id ? (
                          <strong>{membro.nome} (você)</strong>
                        ) : (
                          membro.nome
                        )}
                      </td>
                      <td>{membro.email}</td>
                      <td>
                        {gerencia ? (
                          <select
                            value={membro.papel}
                            onChange={(e) => alterarPapel(membro, e.target.value)}
                            aria-label={`Papel de ${membro.nome}`}
                          >
                            {PAPEIS.map((p) => (
                              <option key={p} value={p}>{p}</option>
                            ))}
                          </select>
                        ) : (
                          <span className={`badge papel-${membro.papel.toLowerCase()}`}>
                            {membro.papel}
                          </span>
                        )}
                      </td>
                      <td>{formatarData(membro.desde)}</td>
                      {gerencia && (
                        <td>
                          <div className="acoes-linha">
                            {membro.usuarioId !== usuario?.id && (
                              <button
                                className="btn secundario"
                                onClick={() => redefinirSenhaDe(membro)}
                                aria-label={`Redefinir senha de ${membro.nome}`}
                              >
                                Redefinir senha
                              </button>
                            )}
                            {membro.usuarioId !== usuario?.id && (
                              <button
                                className="btn perigo"
                                onClick={() => removerMembro(membro)}
                                aria-label={`Remover ${membro.nome}`}
                              >
                                Remover
                              </button>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {gerencia && (
            <div className="card form-card">
              <h3>Código de acesso</h3>
              <p className="dica">
                Compartilhe o código para que novos integrantes entrem no grupo pelo portal usando
                o código de acesso. Quem usar o código entra como visitante.
              </p>
              {codigoAcesso ? (
                <>
                  <div className="linha-input-botao">
                    <input readOnly value={codigoAcesso} aria-label="Código de acesso atual" className="mono" />
                    <button className="btn perigo" type="button" onClick={revogarCodigoAcesso} disabled={revogandoCodigo || gerandoCodigo}>
                      {revogandoCodigo ? 'Revogando…' : 'Revogar código'}
                    </button>
                  </div>
                  <button className="btn secundario" type="button" onClick={gerarCodigoAcesso} disabled={gerandoCodigo || revogandoCodigo}>
                    {gerandoCodigo ? 'Gerando…' : 'Trocar código'}
                  </button>
                </>
              ) : (
                <button className="btn primario" type="button" onClick={gerarCodigoAcesso} disabled={gerandoCodigo}>
                  {gerandoCodigo ? 'Gerando…' : 'Criar código de acesso'}
                </button>
              )}
            </div>
          )}

          <div className="card form-card zona-perigo">
            <h3>Sair do grupo</h3>
            <p className="dica">
              Você deixa de participar desta organização. Se for o último administrador, a saída é
              bloqueada: designe outro administrador antes.
            </p>
            <button
              className={`btn perigo${confirmandoSaida ? ' confirmando' : ''}`}
              type="button"
              onClick={sairDoGrupo}
              disabled={saindo}
            >
              {saindo
                ? 'Saindo…'
                : confirmandoSaida
                  ? 'Clique de novo para confirmar'
                  : 'Sair do grupo'}
            </button>
          </div>

          {eAdminGrupo && (
            <div className="card form-card zona-perigo">
              <h3>Excluir grupo</h3>
              <p className="dica">
                Apaga o grupo e todos os dados de negócio da organização (dotações, fornecedores,
                licitações, contratos, empenhos e créditos). Esta ação não pode ser desfeita.
              </p>
              <button
                className="btn perigo"
                type="button"
                onClick={() => setConfirmandoExclusaoGrupo(true)}
              >
                Excluir grupo
              </button>
            </div>
          )}
        </>
      )}

      {gerencia && aba === 'convites' && (
        <>
          <form className="card form-card" onSubmit={criarConvite}>
            <h3>Criar convite</h3>
            <div className="grade-form">
              <div className="campo-largo">
                <label htmlFor="email-convite">E-mail do convidado</label>
                <div className="linha-input-botao">
                  <input
                    id="email-convite"
                    type="email"
                    value={emailConvite}
                    onChange={(e) => setEmailConvite(e.target.value)}
                    placeholder="colega@prefeitura.gov.br"
                    maxLength={150}
                    required
                  />
                </div>
                <p className="dica">
                  O convite vale por 7 dias. Para entrar por código de acesso, o administrador
                  compartilha o código gerado na aba de membros.
                </p>
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
                    <th>Expira em</th>
                    <th aria-label="Ações">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {convites.map((convite) => (
                    <tr key={convite.id}>
                      <td className="mono">{destinoConvite(convite)}</td>
                      <td>{convite.papel}</td>
                      <td>{formatarData(convite.criadoEm)}</td>
                      <td>{formatarData(convite.expiraEm)}</td>
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

      <ModalConfirmacao
        aberto={membroExclusao !== null}
        titulo="Remover integrante"
        mensagem={
          membroExclusao
            ? `Remover ${membroExclusao.nome} deste grupo? Ele deixará de acessar os dados associados à organização.`
            : ''
        }
        rotuloConfirmar="Remover"
        rotuloCancelar="Cancelar"
        confirmando={removendo}
        aoConfirmar={confirmarRemocao}
        aoCancelar={() => setMembroExclusao(null)}
      />

      <ModalConfirmacao
        aberto={conviteExclusao !== null}
        titulo="Revogar convite"
        mensagem={
          conviteExclusao
            ? `Revogar o convite para "${destinoConvite(conviteExclusao)}"? Quem ainda não usou o convite perderá o acesso.`
            : ''
        }
        rotuloConfirmar="Revogar"
        rotuloCancelar="Cancelar"
        confirmando={revogando}
        aoConfirmar={confirmarRevogacao}
        aoCancelar={() => setConviteExclusao(null)}
      />

      <ModalConfirmacao
        aberto={confirmandoExclusaoGrupo}
        titulo="Excluir grupo"
        mensagem="Tem certeza que deseja excluir este grupo? Todos os dados de negócio da organização serão apagados permanentemente."
        rotuloConfirmar="Excluir grupo"
        rotuloCancelar="Cancelar"
        confirmando={excluindoGrupo}
        aoConfirmar={confirmarExclusaoGrupo}
        aoCancelar={() => setConfirmandoExclusaoGrupo(false)}
      />

      {membroSenha && (
        <ModalRedefinirSenha
          nomeMembro={membroSenha.nome}
          salvando={redefinindoSenha}
          erro={erroSenha}
          aoSalvar={confirmarRedefinicaoSenha}
          aoCancelar={() => {
            setMembroSenha(null)
            setErroSenha(null)
          }}
        />
      )}
    </section>
  )
}