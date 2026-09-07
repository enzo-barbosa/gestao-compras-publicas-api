import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

function linkClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'ativo' : ''
}

export default function Navbar() {
  const { usuario, ehAdmin, logout } = useAuth()
  const navegar = useNavigate()

  function sair() {
    logout()
    navegar('/login')
  }

  return (
    <header className="navbar">
      <div className="marca">
        <span className="logo">GCP</span>
        <span>Compras Públicas</span>
      </div>

      <nav aria-label="Navegação principal">
        <NavLink to="/" end className={linkClass}>Dashboard</NavLink>
        {ehAdmin && (
          <>
            <NavLink to="/dotacoes" className={linkClass}>Dotações</NavLink>
            <NavLink to="/fornecedores" className={linkClass}>Fornecedores</NavLink>
            <NavLink to="/licitacoes" className={linkClass}>Licitações</NavLink>
            <NavLink to="/contratos" className={linkClass}>Contratos</NavLink>
          </>
        )}
        <NavLink to="/empenhos" className={linkClass}>Empenhos</NavLink>
      </nav>

      <div className="usuario-box">
        <span className="nome">{usuario?.nome}</span>
        <span className={`badge ${ehAdmin ? 'admin' : 'comum'}`}>{usuario?.perfil}</span>
        <button className="btn fantasma" type="button" onClick={sair}>
          Sair
        </button>
      </div>
    </header>
  )
}
