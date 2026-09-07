import { useCallback, useEffect, useState } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import EmpenhoForm from '../components/EmpenhoForm'
import { useToast } from '../context/useToast'
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

const PARAMS = { size: 100, sort: 'dataEmissao,desc' } as const

export default function EmpenhosPage() {
  const { exibir } = useToast()
  const [itens, setItens] = useState<Empenho[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [filtroStatus, setFiltroStatus] = useState('')

  const buscar = useCallback(async (): Promise<Empenho[]> => {
    const resposta = await api.get<Pagina<Empenho>>('/empenhos', { params: PARAMS })
    return resposta.data.content ?? []
  }, [])

  const carregar = useCallback(async () => {
    try {
      setItens(await buscar())
      setErro(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setCarregando(false)
    }
  }, [buscar])

  useEffect(() => {
    let ativo = true
    buscar().then(
      (dados) => {
        if (ativo) {
          setItens(dados)
          setErro(null)
        }
      },
      (e) => {
        if (ativo) setErro(extrairMensagemErro(e))
      },
    ).finally(() => {
      if (ativo) setCarregando(false)
    })
    return () => {
      ativo = false
    }
  }, [buscar])

  async function anular(id: number) {
    if (!window.confirm('Confirma a anulação deste empenho? Os valores serão estornados ao contrato e à dotação.')) return
    setErro(null)
    try {
      await api.delete(`/empenhos/${id}`)
      exibir('sucesso', 'Empenho anulado e saldos estornados.')
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
      {erro && <div className="alerta erro" role="alert">{erro}</div>}

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
        ariaLabel="Tabela de empenhos"
        acoes={(e) =>
          e.status === 'EMPENHADO' ? (
            <button className="btn perigo" onClick={() => anular(e.id)} aria-label={`Anular empenho ${e.contratoNumero}/${e.mesReferencia}/${e.anoReferencia}`}>Anular</button>
          ) : (
            <span className="dica">—</span>
          )
        }
      />
    </section>
  )
}