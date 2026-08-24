import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import { extrairMensagemErro, formatarCompetencia, formatarMoeda } from '../utils/format'

interface ContratoVigente {
  id: number
  numero: string
  fornecedorNome: string
  valorMensal: number
}

interface FeedbackSaldos {
  contratoNumero: string
  valorEmpenhado: number
  saldoContrato: number
  saldoDotacao: number
  competencia: string
}

interface Props {
  onGerado: () => void | Promise<void>
}

export default function EmpenhoForm({ onGerado }: Props) {
  const [contratos, setContratos] = useState<ContratoVigente[]>([])
  const hoje = new Date()
  const [contratoId, setContratoId] = useState('')
  const [mes, setMes] = useState(String(hoje.getMonth() + 1))
  const [ano, setAno] = useState(String(hoje.getFullYear()))
  const [erro, setErro] = useState<string | null>(null)
  const [gerando, setGerando] = useState(false)
  const [feedback, setFeedback] = useState<FeedbackSaldos | null>(null)

  useEffect(() => {
    api.get('/contratos', { params: { status: 'VIGENTE', size: 100 } })
      .then((r) => {
        const lista: ContratoVigente[] = r.data.content ?? []
        setContratos(lista)
        if (lista.length > 0) setContratoId(String(lista[0].id))
      })
      .catch(() => undefined)
  }, [])

  const selecionado = contratos.find((c) => String(c.id) === contratoId)

  async function gerar(evento: FormEvent) {
    evento.preventDefault()
    if (!contratoId) return
    setErro(null)
    setFeedback(null)
    setGerando(true)
    try {
      const resposta = await api.post('/empenhos', {
        contratoId: Number(contratoId),
        mesReferencia: Number(mes),
        anoReferencia: Number(ano),
      })
      const empenho = resposta.data
      const [contrato, saldo] = await Promise.all([
        api.get(`/contratos/${empenho.contratoId}`),
        api.get(`/dotacoes/${empenho.dotacaoId}/saldo`),
      ])
      setFeedback({
        contratoNumero: empenho.contratoNumero,
        valorEmpenhado: Number(empenho.valor),
        saldoContrato: Number(contrato.data.saldoRestante),
        saldoDotacao: Number(saldo.data),
        competencia: formatarCompetencia(empenho.mesReferencia, empenho.anoReferencia),
      })
      await onGerado()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setGerando(false)
    }
  }

  return (
    <div className="card form-card">
      <h3>Gerar empenho</h3>
      {feedback && (
        <div className="alerta sucesso feedback-saldos">
          Empenho gerado para <strong>{feedback.competencia}</strong> — contrato{' '}
          <strong>{feedback.contratoNumero}</strong>, débito de{' '}
          <strong>{formatarMoeda(feedback.valorEmpenhado)}</strong>. Saldo restante do
          contrato: <strong>{formatarMoeda(feedback.saldoContrato)}</strong> · Saldo da
          dotação: <strong>{formatarMoeda(feedback.saldoDotacao)}</strong>
        </div>
      )}
      {erro && <div className="alerta erro">{erro}</div>}
      {contratos.length === 0 ? (
        <p className="dica">Nenhum contrato vigente — crie um contrato antes de gerar empenhos.</p>
      ) : (
        <form onSubmit={gerar} className="grade-form">
          <div className="campo-largo">
            <label htmlFor="contrato">Contrato vigente</label>
            <select id="contrato" value={contratoId} onChange={(e) => { setContratoId(e.target.value); setFeedback(null) }} required>
              {contratos.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.numero} — {c.fornecedorNome}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="mes">Mês de referência</label>
            <input id="mes" type="number" min="1" max="12" step="1" value={mes} onChange={(e) => setMes(e.target.value)} required />
          </div>
          <div>
            <label htmlFor="ano">Ano de referência</label>
            <input id="ano" type="number" min="2000" max="2100" step="1" value={ano} onChange={(e) => setAno(e.target.value)} required />
          </div>
          <div className="acoes-form">
            <button className="btn primario" type="submit" disabled={gerando}>
              {gerando ? 'Gerando…' : 'Gerar empenho'}
            </button>
          </div>
          {selecionado && (
            <p className="dica campo-largo">
              O valor mensal deste contrato ({formatarMoeda(selecionado.valorMensal)}) será debitado do saldo do
              contrato e da dotação vinculada, uma única vez por competência.
            </p>
          )}
        </form>
      )}
    </div>
  )
}
