import { Link } from 'react-router-dom'
import TopoPublico from '../components/TopoPublico'

export default function LandingPage() {
  return (
    <div className="tela-landing">
      <TopoPublico />

      <main className="landing-conteudo">
        <section className="hero">
          <p className="eyebrow">Gestão orçamentária para o setor público</p>
          <h1>Compras públicas sem planilha, sem susto no fim do ano.</h1>
          <p className="hero-sub">
            Cadastre dotações e fornecedores, conduza licitações, feche contratos e empenhe
            por competência — com papéis por equipe e o saldo sempre em dia.
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
              <h3>Organize o grupo</h3>
              <p>Crie o grupo do seu órgão e convide colegas por e-mail ou pelo código de acesso.</p>
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
          <h2 className="titulo-secao">Feito para quem cuida do orçamento</h2>
          <div className="grade-recursos">
            <div className="card recurso">
              <span className="recurso-icone">🏢</span>
              <h3>Múltiplos grupos</h3>
              <p>Participe de quantos órgãos precisar e troque o grupo ativo com um clique.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">🛡️</span>
              <h3>Papéis por equipe</h3>
              <p>Administradores, operadores e visitantes com permissões próprias em cada grupo.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">🧾</span>
              <h3>Orçamento rastreável</h3>
              <p>Empenhos ligados a contrato e dotação, com saldo e estorno automáticos.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">📅</span>
              <h3>Só na competência certa</h3>
              <p>O empenho compromete o mês do desembolso, sem travar o orçamento do ano.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">🔒</span>
              <h3>Isolamento por organização</h3>
              <p>Cada grupo enxerga apenas os próprios dados, com acesso controlado por papel.</p>
            </div>
            <div className="card recurso">
              <span className="recurso-icone">✅</span>
              <h3>Auditoria pronta</h3>
              <p>Movimentações e estornos registrados, prontos para o controle interno e o TCE.</p>
            </div>
          </div>
        </section>

        <section className="cta-final">
          <h2>Organize as compras do seu órgão.</h2>
          <p>Leva menos de um minuto para criar a conta e começar.</p>
          <Link className="btn primario grande" to="/cadastro">Criar conta gratuita</Link>
        </section>
      </main>
    </div>
  )
}