import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import {
  definirOrgAtiva,
  organizacaoAtiva,
  orgIdAtiva,
  podeEmitirEmpenho,
} from '../services/organizacoes'

function linkClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'ativo' : ''
}

export default function Navbar() {
  const { usuario, logout } = useAuth()
  const navegar = useNavigate()
  const [menuAberto, setMenuAberto] = useState(false)
  const caixaRef = useRef<HTMLDivElement>(null)

  const organizacoes = usuario?.organizacoes ?? []
  const orgAtiva = organizacaoAtiva(organizacoes, orgIdAtiva())
  const temGrupo = organizacoes.length > 0

  useEffect(() => {
    function fechar(evento: MouseEvent) {
      if (caixaRef.current && !caixaRef.current.contains(evento.target as Node)) {
        setMenuAberto(false)
      }
    }
    document.addEventListener('mousedown', fechar)
    return () => document.removeEventListener('mousedown', fechar)
  }, [])

  function sair() {
    logout()
    navegar('/login')
  }

  function trocarGrupo(id: number) {
    definirOrgAtiva(id)
    navegar('/app')
  }

  const iniciais = (usuario?.nome ?? '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase() ?? '')
    .join('')

  return (
    <header className="navbar">
      <Link to="/app" className="marca" aria-label="Ir para o dashboard">
        <span className="logo">GCP</span>
        <span>Compras Públicas</span>
      </Link>

      <nav aria-label="Navegação principal">
        <NavLink to="/app" end className={linkClass}>Dashboard</NavLink>
        <NavLink to="/app/grupos" className={linkClass}>Grupos</NavLink>
        {temGrupo && (
          <>
            <NavLink to="/app/dotacoes" className={linkClass}>Dotações</NavLink>
            <NavLink to="/app/fornecedores" className={linkClass}>Fornecedores</NavLink>
            <NavLink to="/app/licitacoes" className={linkClass}>Licitações</NavLink>
            <NavLink to="/app/contratos" className={linkClass}>Contratos</NavLink>
            <NavLink to="/app/creditos" className={linkClass}>Créditos</NavLink>
          </>
        )}
        {podeEmitirEmpenho(usuario?.perfil ?? '', orgAtiva?.papel) && (
          <NavLink to="/app/empenhos" className={linkClass}>Empenhos</NavLink>
        )}
        <NavLink to="/app/membros" className={linkClass}>Integrantes</NavLink>
        {usuario?.perfil === 'SUPER_ADMIN' && (
          <NavLink to="/app/superpainel" className={linkClass}>Super admin</NavLink>
        )}
      </nav>

      {temGrupo && (
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

      <div className={`usuario-box${temGrupo ? '' : ' sem-org'}`} ref={caixaRef}>
        <button
          type="button"
          className="menu-usuario"
          aria-haspopup="menu"
          aria-expanded={menuAberto}
          onClick={() => setMenuAberto((aberto) => !aberto)}
        >
          <span className="avatar" aria-hidden="true">{iniciais}</span>
          <span className="nome">{usuario?.nome}</span>
          <span className="seta-menu" aria-hidden="true">▾</span>
        </button>

        {menuAberto && (
          <div className="menu-drop" role="menu" aria-label="Menu do usuário">
            <div className="menu-cabecalho">
              <span className="menu-nome">{usuario?.nome}</span>
              <span className="menu-email">{usuario?.email}</span>
              {orgAtiva && (
                <span className={`badge papel-${orgAtiva.papel.toLowerCase()}`}>
                  {orgAtiva.nome} · {orgAtiva.papel}
                </span>
              )}
            </div>
            <Link
              to="/app/conta"
              className="menu-item"
              role="menuitem"
              onClick={() => setMenuAberto(false)}
            >
              Minha conta
            </Link>
            <button type="button" className="menu-item" role="menuitem" onClick={sair}>
              Sair
            </button>
          </div>
        )}
      </div>
    </header>
  )
}