import { Link } from 'react-router-dom'

export default function LandingPage() {
  return (
    <div className="tela-landing">
      <header className="topo-publico">
        <div className="marca">
          <span className="logo">GCP</span>
          <span>Compras Públicas</span>
        </div>
        <nav className="acoes-topo">
          <Link className="btn fantasma" to="/login">Entrar</Link>
          <Link className="btn primario" to="/cadastro">Criar conta</Link>
        </nav>
      </header>

      <main className="hero">
        <h1>Gestão de compras públicas</h1>
        <p className="hero-sub">
          Cadastro, dotações orçamentárias, licitações, contratos e empenhos
          organizados por grupo — cada órgão com seus dados isolados e
          papéis de acesso por equipe.
        </p>
        <div className="hero-acoes">
          <Link className="btn primario grande" to="/cadastro">Começar agora</Link>
          <Link className="btn secundario grande" to="/login">Já tenho conta</Link>
        </div>

        <div className="grade-recursos">
          <div className="card recurso">
            <span className="recurso-icone">🏢</span>
            <h3>Múltiplos grupos</h3>
            <p>Participe de quantas organizações precisar, trocando o grupo ativo com um clique.</p>
          </div>
          <div className="card recurso">
            <span className="recurso-icone">🛡️</span>
            <h3>Papéis por equipe</h3>
            <p>Administradores, operadores e visitantes com permissões distintas em cada grupo.</p>
          </div>
          <div className="card recurso">
            <span className="recurso-icone">🧾</span>
            <h3>Orçamento rastreável</h3>
            <p>Empenhos atrelados a contrato e dotação, com saldo e estorno automáticos.</p>
          </div>
        </div>
      </main>

      <footer className="rodape-publico">
        Gestão de Compras Públicas · Israel Mota · estudo técnico
      </footer>
    </div>
  )
}