import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

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
        <NavLink to="/" end>Dashboard</NavLink>
        {ehAdmin && (
          <>
            <NavLink to="/dotacoes">Dotações</NavLink>
            <NavLink to="/fornecedores">Fornecedores</NavLink>
            <NavLink to="/licitacoes">Licitações</NavLink>
            <NavLink to="/contratos">Contratos</NavLink>
          </>
        )}
        <NavLink to="/empenhos">Empenhos</NavLink>
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
