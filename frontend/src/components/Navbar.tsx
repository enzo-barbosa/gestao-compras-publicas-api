import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import {
  definirOrgAtiva,
  organizacaoAtiva,
  orgIdAtiva,
  podeEmitirEmpenho,
  podeGerir,
} from '../services/organizacoes'

function linkClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'ativo' : ''
}

export default function Navbar() {
  const { usuario, logout } = useAuth()
  const navegar = useNavigate()

  const organizacoes = usuario?.organizacoes ?? []
  const orgAtiva = organizacaoAtiva(organizacoes, orgIdAtiva())

  function sair() {
    logout()
    navegar('/login')
  }

  function trocarGrupo(id: number) {
    definirOrgAtiva(id)
    navegar('/app')
  }

  return (
    <header className="navbar">
      <div className="marca">
        <span className="logo">GCP</span>
        <span>Compras Públicas</span>
      </div>

      <nav aria-label="Navegação principal">
        <NavLink to="/app" end className={linkClass}>Dashboard</NavLink>
        {podeGerir(usuario?.perfil ?? '', orgAtiva?.papel) && (
          <>
            <NavLink to="/app/dotacoes" className={linkClass}>Dotações</NavLink>
            <NavLink to="/app/fornecedores" className={linkClass}>Fornecedores</NavLink>
            <NavLink to="/app/licitacoes" className={linkClass}>Licitações</NavLink>
            <NavLink to="/app/contratos" className={linkClass}>Contratos</NavLink>
          </>
        )}
        {podeEmitirEmpenho(usuario?.perfil ?? '', orgAtiva?.papel) && (
          <NavLink to="/app/empenhos" className={linkClass}>Empenhos</NavLink>
        )}
        {podeGerir(usuario?.perfil ?? '', orgAtiva?.papel) && (
          <NavLink to="/app/membros" className={linkClass}>Membros</NavLink>
        )}
        {usuario?.perfil === 'SUPER_ADMIN' && (
          <NavLink to="/app/superpainel" className={linkClass}>Super admin</NavLink>
        )}
      </nav>

      {organizacoes.length > 0 && (
        <label className="org-box">
          <span className="org-rotulo">Grupo</span>
          <select
            value={orgAtiva?.id ?? ''}
            onChange={(e) => trocarGrupo(Number(e.target.value))}
            aria-label="Grupo ativo"
          >
            {organizacoes.map((org) => (
              <option key={org.id} value={org.id}>
                {org.nome} · {org.papel}
              </option>
            ))}
          </select>
        </label>
      )}

      <div className="usuario-box">
        <span className="nome">{usuario?.nome}</span>
        {orgAtiva && (
          <span className={`badge papel-${orgAtiva.papel.toLowerCase()}`}>{orgAtiva.papel}</span>
        )}
        <button className="btn fantasma" type="button" onClick={sair}>
          Sair
        </button>
      </div>
    </header>
  )
}