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

      <main className="landing-conteudo">
        <section className="hero">
          <p className="eyebrow">Gestão orçamentária para órgãos públicos</p>
          <h1>Cadastre, licite, contrate e empenhe com controle e rastreabilidade.</h1>
          <p className="hero-sub">
            Dotações orçamentárias, fornecedores, licitações, contratos e empenhos em um só
            lugar — organizados por grupo, com papéis de acesso por equipe e lançamentos que
            comprometem o orçamento apenas na competência do desembolso.
          </p>
          <div className="hero-acoes">
            <Link className="btn primario grande" to="/cadastro">Começar agora</Link>
            <Link className="btn secundario grande" to="/login">Já tenho conta</Link>
          </div>
        </section>

        <section className="secao como-funciona">
          <h2 className="titulo-secao">Como funciona</h2>
          <div className="grade-passos">
            <div className="passo">
              <span className="numero-passo">1</span>
              <h3>Crie ou entre no grupo</h3>
              <p>Crie a organização do seu órgão e convide colegas por e-mail ou código.</p>
            </div>
            <div className="passo">
              <span className="numero-passo">2</span>
              <h3>Monte a cadeia de compras</h3>
              <p>Dotações, fornecedores, licitações com vencedor e contratos por competência.</p>
            </div>
            <div className="passo">
              <span className="numero-passo">3</span>
              <h3>Emita empenhos sem susto</h3>
              <p>Crédito por mês sobre o saldo real da dotação, com anulação e estorno automáticos.</p>
            </div>
          </div>
        </section>

        <section className="secao recursos">
          <h2 className="titulo-secao">Por que o GCP</h2>
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
            <div className="card recurso">
              <span className="recurso-icone">📅</span>
              <h3>Só na competência certa</h3>
              <p>Não trava o orçamento do ano inteiro: cada empenho compromete o valor do mês.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">🔒</span>
              <h3>Isolamento por organização</h3>
              <p>Cada grupo enxerga somente os próprios dados — com papéis controlados por membro.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">✓</span>
              <h3>Auditoria pronta</h3>
              <p>Histórico de movimentações por dotação e estornos rastreáveis em cada lançamento.</p>
            </div>
          </div>
        </section>

        <section className="cta-final">
          <h2>Pronto para organizar as compras do seu órgão?</h2>
          <p>Leva menos de um minuto para criar a conta e o primeiro grupo.</p>
          <Link className="btn primario grande" to="/cadastro">Criar conta gratuita</Link>
        </section>
      </main>

      <footer className="rodape-publico">
        Gestão de Compras Públicas · Israel Mota · estudo técnico
      </footer>
    </div>
  )
}