import { useEffect, useState } from 'react'
import api from '../services/api'
import { formatarCompetencia, formatarMoeda, formatarStatusEmpenho, extrairMensagemErro } from '../utils/format'

interface DotacaoResumo {
  id: number
  codigo: string
  descricao: string
  saldoAtual: number
}

interface EmpenhoRecente {
  id: number
  contratoNumero: string
  mesReferencia: number
  anoReferencia: number
  valor: number
  status: string
}

export default function DashboardPage() {
  const [dotacoes, setDotacoes] = useState<DotacaoResumo[]>([])
  const [empenhos, setEmpenhos] = useState<EmpenhoRecente[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    async function carregar() {
      try {
        const [respostaDotacoes, respostaEmpenhos] = await Promise.all([
          api.get('/dotacoes', { params: { size: 50 } }),
          api.get('/empenhos', { params: { size: 6, sort: 'dataEmissao,desc' } }),
        ])
        setDotacoes(respostaDotacoes.data.content ?? [])
        setEmpenhos(respostaEmpenhos.data.content ?? [])
      } catch (e) {
        setErro(extrairMensagemErro(e))
      } finally {
        setCarregando(false)
      }
    }
    void carregar()
  }, [])

  if (carregando) {
    return <p className="vazio">Carregando…</p>
  }

  return (
    <section>
      <h2>Dashboard</h2>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}

      <h3>Saldos por dotação orçamentária</h3>
      {dotacoes.length === 0 ? (
        <p className="vazio">Nenhuma dotação cadastrada.</p>
      ) : (
        <div className="grade-saldos">
          {dotacoes.map((d) => (
            <div key={d.id} className="card card-saldo">
              <span className="codigo">{d.codigo}</span>
              <span className="descricao">{d.descricao}</span>
              <span className={`saldo ${d.saldoAtual <= 0 ? 'zerado' : ''}`}>
                {formatarMoeda(d.saldoAtual)}
              </span>
            </div>
          ))}
        </div>
      )}

      <h3>Empenhos recentes</h3>
      {empenhos.length === 0 ? (
        <p className="vazio">Nenhum empenho gerado ainda.</p>
      ) : (
        <table className="tabela">
          <thead>
            <tr>
              <th>Contrato</th>
              <th>Competência</th>
              <th>Valor</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {empenhos.map((e) => (
              <tr key={e.id}>
                <td>{e.contratoNumero}</td>
                <td>{formatarCompetencia(e.mesReferencia, e.anoReferencia)}</td>
                <td>{formatarMoeda(e.valor)}</td>
                <td>{formatarStatusEmpenho(e.status)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
