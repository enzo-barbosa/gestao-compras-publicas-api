import { useCallback, useEffect, useMemo, useState } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import ModalConfirmacao from '../components/ModalConfirmacao'
import EmpenhoForm from '../components/EmpenhoForm'
import { useToast } from '../context/useToast'
import { useAuth } from '../context/useAuth'
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
}

const FILTROS = [
  ['', 'Todos'],
  ['EMPENHADO', 'Empenhados'],
  ['ANULADO', 'Anulados'],
] as const

const ANO_ATUAL = new Date().getFullYear()

const ANOS = Array.from({ length: 6 }, (_, i) => ANO_ATUAL + 1 - i)

const MESES = [
  ['', 'Todos os meses'],
  ['1', 'Janeiro'],
  ['2', 'Fevereiro'],
  ['3', 'Março'],
  ['4', 'Abril'],
  ['5', 'Maio'],
  ['6', 'Junho'],
  ['7', 'Julho'],
  ['8', 'Agosto'],
  ['9', 'Setembro'],
  ['10', 'Outubro'],
  ['11', 'Novembro'],
  ['12', 'Dezembro'],
] as const

export default function EmpenhosPage() {
  const { exibir } = useToast()
  const { podeOperar } = useAuth()
  const [itens, setItens] = useState<Empenho[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [filtroStatus, setFiltroStatus] = useState('')
  const [filtroMes, setFiltroMes] = useState('')
  const [filtroAno, setFiltroAno] = useState('')
  const [anulando, setAnulando] = useState<Empenho | null>(null)
  const [confirmandoAnulacao, setConfirmandoAnulacao] = useState(false)

  const params = useMemo(() => ({
    size: 100,
    sort: 'dataEmissao,desc',
    ...(filtroMes ? { mes: Number(filtroMes) } : {}),
    ...(filtroAno ? { ano: Number(filtroAno) } : {}),
  }), [filtroMes, filtroAno])

  const buscar = useCallback(async (): Promise<Empenho[]> => {
    const resposta = await api.get<Pagina<Empenho>>('/empenhos', { params })
    return resposta.data.content ?? []
  }, [params])

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

  async function confirmarAnulacao() {
    if (!anulando) return
    setErro(null)
    setConfirmandoAnulacao(true)
    try {
      await api.delete(`/empenhos/${anulando.id}`)
      exibir('sucesso', 'Empenho anulado e saldos estornados.')
      setAnulando(null)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setAnulando(null)
    } finally {
      setConfirmandoAnulacao(false)
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
  ]

  return (
    <section>
      <h2>Empenhos</h2>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}

      {podeOperar && <EmpenhoForm onGerado={carregar} />}

      <div className="barra-filtros">
        <select aria-label="Filtrar por mês" value={filtroMes} onChange={(e) => setFiltroMes(e.target.value)}>
          {MESES.map(([valor, rotulo]) => (
            <option key={valor} value={valor}>{rotulo}</option>
          ))}
        </select>
        <select aria-label="Filtrar por ano" value={filtroAno} onChange={(e) => setFiltroAno(e.target.value)}>
          <option value="">Todos os anos</option>
          {ANOS.map((ano) => (
            <option key={ano} value={ano}>{ano}</option>
          ))}
        </select>
      </div>

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
        acoes={
          podeOperar
            ? (e) =>
                e.status === 'EMPENHADO' ? (
                  <button className="btn perigo" onClick={() => setAnulando(e)} aria-label={`Anular empenho ${e.contratoNumero}/${e.mesReferencia}/${e.anoReferencia}`}>Anular</button>
                ) : (
                  <span className="dica">—</span>
                )
            : undefined
        }
      />

      <ModalConfirmacao
        aberto={anulando !== null}
        titulo="Anular empenho"
        mensagem={
          anulando
            ? `Confirma a anulação do empenho ${anulando.contratoNumero}/${anulando.mesReferencia}/${anulando.anoReferencia}? Os valores serão estornados ao contrato e à dotação.`
            : ''
        }
        rotuloConfirmar="Anular"
        rotuloCancelar="Cancelar"
        confirmando={confirmandoAnulacao}
        aoConfirmar={confirmarAnulacao}
        aoCancelar={() => setAnulando(null)}
      />
    </section>
  )
}