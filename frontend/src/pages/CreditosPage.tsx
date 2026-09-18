import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import { extrairMensagemErro, formatarData, formatarMoeda } from '../utils/format'
import { paramsListagem } from '../utils/listagem'

interface Credito {
  id: number
  dotacaoOrigemCodigo: string
  dotacaoDestinoCodigo: string
  valor: number
  descricao: string
  data: string
}

interface DotacaoOpcao {
  id: number
  codigo: string
}

export default function CreditosPage() {
  const { podeOperar } = useAuth()
  const { exibir } = useToast()
  const [itens, setItens] = useState<Credito[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [dotacoes, setDotacoes] = useState<DotacaoOpcao[]>([])
  const [filtroDotacaoId, setFiltroDotacaoId] = useState('')
  const [filtroDataInicio, setFiltroDataInicio] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')
  const [origemId, setOrigemId] = useState('')
  const [destinoId, setDestinoId] = useState('')
  const [valor, setValor] = useState('')
  const [descricao, setDescricao] = useState('')
  const [data, setData] = useState(() => {
    const agora = new Date()
    const ajustado = new Date(agora.getTime() - agora.getTimezoneOffset() * 60000)
    return ajustado.toISOString().slice(0, 10)
  })
  const [registrando, setRegistrando] = useState(false)

  const params = useMemo(
    () => ({
      ...paramsListagem('data,desc'),
      ...(filtroDotacaoId ? { dotacaoId: Number(filtroDotacaoId) } : {}),
      ...(filtroDataInicio ? { dataInicio: filtroDataInicio } : {}),
      ...(filtroDataFim ? { dataFim: filtroDataFim } : {}),
    }),
    [filtroDotacaoId, filtroDataInicio, filtroDataFim],
  )

  const buscar = useCallback(async (): Promise<Credito[]> => {
    const resposta = await api.get<Pagina<Credito>>('/creditos-suplementares', { params })
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

  useEffect(() => {
    api
      .get<Pagina<DotacaoOpcao>>('/dotacoes', { params: { size: 100 } })
      .then((r) => {
        const lista = r.data.content ?? []
        setDotacoes(lista)
        if (lista.length > 0) {
          setOrigemId((atual) => atual || String(lista[0]?.id))
          const segundo = lista.length > 1 ? lista[1] : null
          setDestinoId((atual) => atual || (segundo ? String(segundo.id) : ''))
        }
      })
      .catch(() => undefined)
  }, [])

  async function registrar(evento: FormEvent) {
    evento.preventDefault()
    if (!origemId || !destinoId) {
      setErro('Selecione as dotações de origem e de destino.')
      return
    }
    if (origemId === destinoId) {
      setErro('A dotação de origem deve ser diferente da de destino.')
      return
    }
    const valorNumerico = Number(valor)
    if (!Number.isFinite(valorNumerico) || valorNumerico <= 0) {
      setErro('Informe um valor maior que zero.')
      return
    }
    setErro(null)
    setRegistrando(true)
    try {
      await api.post('/creditos-suplementares', {
        dotacaoOrigemId: Number(origemId),
        dotacaoDestinoId: Number(destinoId),
        valor: valorNumerico,
        descricao: descricao.trim() || undefined,
        data: data || undefined,
      })
      exibir('sucesso', 'Crédito suplementar registrado e saldos atualizados.')
      setValor('')
      setDescricao('')
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setRegistrando(false)
    }
  }

  const origemSelecionada = origemId ? Number(origemId) : null

  const colunas: Coluna<Credito>[] = [
    { key: 'data', label: 'Data', render: (c) => formatarData(c.data) },
    { key: 'dotacaoOrigemCodigo', label: 'Origem', render: (c) => <strong>{c.dotacaoOrigemCodigo}</strong> },
    { key: 'dotacaoDestinoCodigo', label: 'Destino' },
    { key: 'valor', label: 'Valor', render: (c) => formatarMoeda(c.valor) },
    { key: 'descricao', label: 'Descrição' },
  ]

  return (
    <section>
      <h2>Créditos suplementares</h2>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}
      <p className="dica">
        Transfere saldo entre dotações: a origem é debitada e o destino é creditado no mesmo valor.
      </p>

      <div className="barra-filtros">
        <select aria-label="Filtrar créditos por dotação" value={filtroDotacaoId} onChange={(e) => setFiltroDotacaoId(e.target.value)}>
          <option value="">Todas as dotações</option>
          {dotacoes.map((d) => (
            <option key={d.id} value={d.id}>{d.codigo}</option>
          ))}
        </select>
        <label className="filtro-data">
          De
          <input type="date" aria-label="Data inicial" value={filtroDataInicio} onChange={(e) => setFiltroDataInicio(e.target.value)} />
        </label>
        <label className="filtro-data">
          Até
          <input type="date" aria-label="Data final" value={filtroDataFim} onChange={(e) => setFiltroDataFim(e.target.value)} />
        </label>
      </div>

      {podeOperar && (
        <div className="card form-card">
          <h3>Registrar crédito suplementar</h3>
          {dotacoes.length < 2 ? (
            <p className="dica">
              Você precisa de ao menos duas dotações para transferir saldo entre elas.
            </p>
          ) : (
            <form onSubmit={registrar} className="grade-form">
              <div>
                <label htmlFor="origem">Dotação de origem</label>
                <select
                  id="origem"
                  value={origemId}
                  onChange={(e) => { setOrigemId(e.target.value); setErro(null) }}
                  required
                >
                  {dotacoes.filter((d) => d.id !== (destinoId ? Number(destinoId) : null)).map((d) => (
                    <option key={d.id} value={d.id}>{d.codigo}</option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="destino">Dotação de destino</label>
                <select
                  id="destino"
                  value={destinoId}
                  onChange={(e) => { setDestinoId(e.target.value); setErro(null) }}
                  required
                >
                  {dotacoes.filter((d) => d.id !== origemSelecionada).map((d) => (
                    <option key={d.id} value={d.id}>{d.codigo}</option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="valor">Valor (R$)</label>
                <input
                  id="valor"
                  type="number"
                  min="0.01"
                  step="0.01"
                  placeholder="0,00"
                  value={valor}
                  onChange={(e) => { setValor(e.target.value); setErro(null) }}
                  required
                />
              </div>
              <div>
                <label htmlFor="data">Data</label>
                <input
                  id="data"
                  type="date"
                  value={data}
                  onChange={(e) => setData(e.target.value)}
                  required
                />
              </div>
              <div className="campo-largo">
                <label htmlFor="descricao">Descrição</label>
                <input
                  id="descricao"
                  type="text"
                  value={descricao}
                  onChange={(e) => setDescricao(e.target.value)}
                  placeholder="Ex.: Reforço da rubrica de manutenção"
                  maxLength={200}
                />
              </div>
              <div className="acoes-form">
                <button className="btn primario" type="submit" disabled={registrando}>
                  {registrando ? 'Registrando…' : 'Registrar'}
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      <TabelaGenerica
        colunas={colunas}
        itens={itens}
        carregando={carregando}
        mensagemVazio="Nenhum crédito suplementar registrado."
        ariaLabel="Tabela de créditos suplementares"
      />
    </section>
  )
}