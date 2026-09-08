import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import api from '../services/api'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import { extrairMensagemErro, formatarData } from '../utils/format'

interface AdminUsuario {
  id: number
  nome: string
  email: string
  perfil: string
}

interface AdminOrganizacao {
  id: number
  nome: string
  criadoEm: string
  totalMembros: number
}

const PERFIS = ['SUPER_ADMIN', 'USUARIO'] as const
type Aba = 'usuarios' | 'organizacoes'

export default function SuperPainelPage() {
  const { usuario } = useAuth()
  const { exibir } = useToast()
  const [aba, setAba] = useState<Aba>('usuarios')
  const [erro, setErro] = useState<string | null>(null)

  const [usuarios, setUsuarios] = useState<AdminUsuario[]>([])
  const [carregandoUsuarios, setCarregandoUsuarios] = useState(true)

  const [organizacoes, setOrganizacoes] = useState<AdminOrganizacao[]>([])
  const [carregandoOrganizacoes, setCarregandoOrganizacoes] = useState(false)

  const buscarUsuarios = useCallback(async (): Promise<AdminUsuario[]> => {
    const resposta = await api.get<AdminUsuario[]>('/admin/usuarios')
    return resposta.data
  }, [])

  const buscarOrganizacoes = useCallback(async (): Promise<AdminOrganizacao[]> => {
    const resposta = await api.get<AdminOrganizacao[]>('/admin/organizacoes')
    return resposta.data
  }, [])

  useEffect(() => {
    let ativo = true
    buscarUsuarios().then(
      (dados) => {
        if (ativo) {
          setUsuarios(dados)
          setErro(null)
        }
      },
      (e) => {
        if (ativo) setErro(extrairMensagemErro(e))
      },
    ).finally(() => {
      if (ativo) setCarregandoUsuarios(false)
    })
    return () => {
      ativo = false
    }
  }, [buscarUsuarios])

  function abrirOrganizacoes() {
    setAba('organizacoes')
    setCarregandoOrganizacoes(true)
    buscarOrganizacoes().then(
      (dados) => {
        setOrganizacoes(dados)
        setErro(null)
      },
      (e) => setErro(extrairMensagemErro(e)),
    ).finally(() => setCarregandoOrganizacoes(false))
  }

  async function alterarPerfil(alvo: AdminUsuario, perfil: string) {
    if (perfil === alvo.perfil) return
    setErro(null)
    try {
      await api.put(`/admin/usuarios/${alvo.id}/perfil`, { perfil })
      exibir('sucesso', `${alvo.nome} agora é ${perfil}.`)
      setUsuarios((atuais) =>
        atuais.map((u) => (u.id === alvo.id ? { ...u, perfil } : u)),
      )
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  if (usuario?.perfil !== 'SUPER_ADMIN') {
    return <Navigate to="/app" replace />
  }

  return (
    <section>
      <h2>Painel de super administrador</h2>
      <p className="dica">
        Visão global de usuários e organizações. Você não pode rebaixar a si mesmo nem o último
        super administrador do sistema.
      </p>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}

      <div className="abas">
        <button
          type="button"
          className={`btn fantasma ${aba === 'usuarios' ? 'ativo' : ''}`}
          onClick={() => setAba('usuarios')}
        >
          Usuários
        </button>
        <button
          type="button"
          className={`btn fantasma ${aba === 'organizacoes' ? 'ativo' : ''}`}
          onClick={abrirOrganizacoes}
        >
          Organizações
        </button>
      </div>

      {aba === 'usuarios' && (
        carregandoUsuarios ? (
          <p className="vazio">Carregando…</p>
        ) : (
          <div className="tabela-wrapper">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Perfil</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map((u) => (
                  <tr key={u.id}>
                    <td>{u.nome}</td>
                    <td>{u.email}</td>
                    <td>
                      <select
                        value={u.perfil}
                        onChange={(e) => alterarPerfil(u, e.target.value)}
                        aria-label={`Perfil de ${u.nome}`}
                      >
                        {PERFIS.map((p) => (
                          <option key={p} value={p}>{p}</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {aba === 'organizacoes' && (
        carregandoOrganizacoes ? (
          <p className="vazio">Carregando…</p>
        ) : (
          <div className="tabela-wrapper">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Nome</th>
                  <th>Criado em</th>
                  <th>Membros</th>
                </tr>
              </thead>
              <tbody>
                {organizacoes.map((org) => (
                  <tr key={org.id}>
                    <td>{org.id}</td>
                    <td>{org.nome}</td>
                    <td>{formatarData(org.criadoEm)}</td>
                    <td>{org.totalMembros}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </section>
  )
}