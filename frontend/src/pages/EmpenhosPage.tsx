import { useEffect, useState } from 'react'
import api from '../services/api'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import EmpenhoForm from '../components/EmpenhoForm'
import { extrairMensagemErro, formatarCompetencia, formatarData, formatarMoeda, formatarStatusEmpenho } from '../utils/format'

interface Empenho {
  id: number
  contratoNumero: string
  dotacaoCodigo: string
  fornecedorNome: string
  mesReferencia: number
  anoReferencia: number
  valor: string | number
  status: string
  dataEmissao: string
  usuarioId: number
}

const FILTROS = [
  ['', 'Todos'],
  ['EMPENHADO', 'Empenhados'],
  ['ANULADO', 'Anulados'],
] as const

export default function EmpenhosPage() {
  const [itens, setItens] = useState<Empenho[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [filtroStatus, setFiltroStatus] = useState('')

  function carregar() {
    return api
      .get('/empenhos', {
        params: { size: 100, sort: 'dataEmissao,desc' },
      })
      .then((resposta) => {
        setItens(resposta.data.content ?? [])
        setErro(null)
      })
      .catch((e) => {
        setErro(extrairMensagemErro(e))
      })
      .finally(() => {
        setCarregando(false)
      })
  }

  useEffect(() => {
    void carregar()
  }, [])

  async function anular(id: number) {
    if (!window.confirm('Confirma a anulação deste empenho? Os valores serão estornados ao contrato e à dotação.')) return
    setErro(null)
    try {
      await api.delete(`/empenhos/${id}`)
      setCarregando(true)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  const filtrados = filtroStatus ? itens.filter((e) => e.status === filtroStatus) : itens

  const colunas: Coluna<Empenho>[] = [
    {
      key: 'competencia',
      label: 'Competência',
      render: (e) => <strong>{formatarCompetencia(e.mesReferencia, e.anoReferencia)}</strong>,
    },
    { key: 'contratoNumero', label: 'Contrato' },
    { key: 'fornecedorNome', label: 'Fornecedor' },
    { key: 'dotacaoCodigo', label: 'Dotação' },
    { key: 'dataEmissao', label: 'Emissão', render: (e) => formatarData(e.dataEmissao) },
    { key: 'valor', label: 'Valor', render: (e) => formatarMoeda(Number(e.valor)) },
    {
      key: 'status',
      label: 'Status',
      render: (e) => (
        <span className={`badge status-${e.status.toLowerCase()}`}>
          {formatarStatusEmpenho(e.status)}
        </span>
      ),
    },
    { key: 'usuarioId', label: 'Usuário', render: (e) => `#${e.usuarioId}` },
  ]

  return (
    <section>
      <h2>Empenhos</h2>
      {erro && <div className="alerta erro">{erro}</div>}

      <EmpenhoForm onGerado={carregar} />

      <div className="barra-filtros">
        {FILTROS.map(([valor, rotulo]) => (
          <button
            key={valor}
            className={`btn fantasma ${filtroStatus === valor ? 'ativo' : ''}`}
            onClick={() => setFiltroStatus(valor)}
          >
            {rotulo}
          </button>
        ))}
      </div>

      <TabelaGenerica
        colunas={colunas}
        itens={filtrados}
        carregando={carregando}
        mensagemVazio="Nenhum empenho registrado."
        acoes={(e) =>
          e.status === 'EMPENHADO' ? (
            <button className="btn perigo" onClick={() => anular(e.id)}>Anular</button>
          ) : (
            <span className="dica">—</span>
          )
        }
      />
    </section>
  )
}
