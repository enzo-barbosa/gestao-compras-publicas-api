import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { definirOrgAtiva, organizacaoAtiva, orgIdAtiva } from '../services/organizacoes'

export default function SelecionarGrupoPage() {
  const { usuario } = useAuth()
  const navegar = useNavigate()

  const organizacoes = useMemo(() => usuario?.organizacoes ?? [], [usuario])
  const ativa = organizacaoAtiva(organizacoes, orgIdAtiva())

  useEffect(() => {
    if (organizacoes.length === 1 && !ativa) {
      definirOrgAtiva(organizacoes[0]?.id ?? null)
      navegar('/app', { replace: true })
    }
  }, [organizacoes, ativa, navegar])

  function escolher(id: number) {
    definirOrgAtiva(id)
    navegar('/app')
  }

  return (
    <div className="tela-auxiliar">
      <div className="card card-seletor">
        <h1>Escolher grupo</h1>
        <p className="subtitulo">Selecione em qual organização deseja trabalhar.</p>

        {organizacoes.length === 0 ? (
          <p className="vazio">Você ainda não participa de nenhum grupo.</p>
        ) : (
          <ul className="lista-grupos">
            {organizacoes.map((org) => (
              <li key={org.id}>
                <button
                  type="button"
                  className="item-grupo"
                  onClick={() => escolher(org.id)}
                  aria-label={`Trabalhar em ${org.nome}`}
                >
                  <span className="grupo-nome">{org.nome}</span>
                  <span className={`badge papel-${org.papel.toLowerCase()}`}>{org.papel}</span>
                </button>
              </li>
            ))}
          </ul>
        )}

        <button className="btn secundario" type="button" onClick={() => navegar('/onboarding')}>
          Criar novo grupo ou aceitar convite
        </button>
      </div>
    </div>
  )
}