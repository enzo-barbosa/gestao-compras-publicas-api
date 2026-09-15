import { Link } from 'react-router-dom'

export default function TopoPublico() {
  return (
    <header className="topo-publico">
      <Link to="/" className="marca" aria-label="Página inicial">
        <span className="logo">GCP</span>
        <span>Compras Públicas</span>
      </Link>
      <nav className="acoes-topo" aria-label="Acesso">
        <Link className="btn fantasma" to="/login">Entrar</Link>
        <Link className="btn primario" to="/cadastro">Criar conta</Link>
      </nav>
    </header>
  )
}