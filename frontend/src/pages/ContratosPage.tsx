import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { extrairMensagemErro, formatarData, formatarMoeda } from '../utils/format'

interface Contrato {
  id: number
  numero: string
  objeto: string
  valorTotal: number
  duracaoMeses: number
  dataInicio: string
  dataFimPrevista: string
  status: string
  saldoRestante: number
  valorMensal: number
  dotacaoId: number
  dotacaoCodigo: string
  fornecedorId: number
  fornecedorNome: string
  licitacaoId: number | null
  licitacaoNumeroEdital: string | null
}

interface DotacaoOpcao {
  id: number
  codigo: string
  saldoAtual: number
}

interface FornecedorOpcao {
  id: number
  nome: string
}

interface LicitacaoOpcao {
  id: number
  numeroEdital: string
  status: string
}

const FORM_VAZIO = {
  numero: '',
  objeto: '',
  valorTotal: '',
  duracaoMeses: '12',
  dataInicio: new Date().toISOString().slice(0, 10),
  dotacaoId: '',
  licitacaoId: '',
  fornecedorId: '',
}

export default function ContratosPage() {
  const { ehAdmin } = useAuth()
  const [itens, setItens] = useState<Contrato[]>([])
  const [dotacoes, setDotacoes] = useState<DotacaoOpcao[]>([])
  const [fornecedores, setFornecedores] = useState<FornecedorOpcao[]>([])
  const [licitacoes, setLicitacoes] = useState<LicitacaoOpcao[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)
  const [form, setForm] = useState(FORM_VAZIO)
  const [editandoId, setEditandoId] = useState<number | null>(null)

  async function carregar() {
    setCarregando(true)
    try {
      const resposta = await api.get('/contratos', { params: { size: 100, sort: 'dataInicio,desc' } })
      setItens(resposta.data.content ?? [])
      setErro(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    void carregar()
    if (ehAdmin) {
      api.get('/dotacoes', { params: { size: 200 } })
        .then((r) => setDotacoes(r.data.content ?? []))
        .catch(() => undefined)
      api.get('/fornecedores', { params: { size: 200 } })
        .then((r) => setFornecedores(r.data.content ?? []))
        .catch(() => undefined)
      api.get('/licitacoes', { params: { size: 200 } })
        .then((r) => setLicitacoes(r.data.content ?? []))
        .catch(() => undefined)
    }
  }, [ehAdmin])

  function iniciarEdicao(c: Contrato) {
    setEditandoId(c.id)
    setForm({
      numero: c.numero,
      objeto: c.objeto,
      valorTotal: String(c.valorTotal),
      duracaoMeses: String(c.duracaoMeses),
      dataInicio: c.dataInicio,
      dotacaoId: String(c.dotacaoId),
      licitacaoId: c.licitacaoId === null ? '' : String(c.licitacaoId),
      fornecedorId: String(c.fornecedorId),
    })
  }

  function cancelar() {
    setEditandoId(null)
    setForm(FORM_VAZIO)
  }

  async function salvar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setSucesso(null)
    try {
      if (editandoId === null) {
        const corpo = {
          numero: form.numero,
          objeto: form.objeto,
          valorTotal: Number(form.valorTotal),
          duracaoMeses: Number(form.duracaoMeses),
          dataInicio: form.dataInicio,
          dotacaoId: Number(form.dotacaoId),
          licitacaoId: form.licitacaoId ? Number(form.licitacaoId) : null,
          fornecedorId: Number(form.fornecedorId),
        }
        await api.post('/contratos', corpo)
        setSucesso('Contrato criado — saldo restante inicializado com o valor total.')
      } else {
        const existente = itens.find((c) => c.id === editandoId)!
        await api.put(`/contratos/${editandoId}`, {
          numero: form.numero,
          objeto: form.objeto,
          valorTotal: existente.valorTotal,
          duracaoMeses: existente.duracaoMeses,
          dataInicio: form.dataInicio,
          dotacaoId: existente.dotacaoId,
          licitacaoId: existente.licitacaoId,
          fornecedorId: existente.fornecedorId,
        })
        setSucesso('Contrato atualizado (número, objeto e data de início são editáveis).')
      }
      cancelar()
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function excluir(id: number) {
    if (!window.confirm('Confirma a exclusão deste contrato?')) return
    setErro(null)
    setSucesso(null)
    try {
      await api.delete(`/contratos/${id}`)
      setSucesso('Contrato removido.')
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  const colunas: Coluna<Contrato>[] = [
    { key: 'numero', label: 'Número' },
    { key: 'fornecedorNome', label: 'Fornecedor' },
    { key: 'dotacaoCodigo', label: 'Dotação' },
    {
      key: 'licitacaoNumeroEdital',
      label: 'Licitação',
      render: (c) => c.licitacaoNumeroEdital ?? '—',
    },
    { key: 'valorTotal', label: 'Valor total', render: (c) => formatarMoeda(c.valorTotal) },
    { key: 'valorMensal', label: 'Valor mensal', render: (c) => formatarMoeda(c.valorMensal) },
    {
      key: 'saldoRestante',
      label: 'Saldo restante',
      render: (c) => <strong className={c.saldoRestante <= 0 ? 'texto-vermelho' : 'texto-verde'}>{formatarMoeda(c.saldoRestante)}</strong>,
    },
    { key: 'dataFimPrevista', label: 'Fim previsto', render: (c) => formatarData(c.dataFimPrevista) },
    {
      key: 'status',
      label: 'Status',
      render: (c) => (
        <span className={`badge status-${c.status.toLowerCase()}`}>
          {c.status.charAt(0) + c.status.slice(1).toLowerCase()}
        </span>
      ),
    },
  ]

  const precisaVinculos = editandoId === null

  return (
    <section>
      <h2>Contratos</h2>
      {erro && <div className="alerta erro">{erro}</div>}
      {sucesso && <div className="alerta sucesso">{sucesso}</div>}

      {ehAdmin && (
        <div className="card form-card">
          <h3>{editandoId === null ? 'Novo contrato' : `Editando contrato #${editandoId}`}</h3>
          <form onSubmit={salvar} className="grade-form">
            <div>
              <label htmlFor="numero">Número</label>
              <input id="numero" value={form.numero} onChange={(e) => setForm({ ...form, numero: e.target.value })} placeholder="015/2026" required maxLength={30} />
            </div>
            <div className="campo-largo">
              <label htmlFor="objeto">Objeto</label>
              <input id="objeto" value={form.objeto} onChange={(e) => setForm({ ...form, objeto: e.target.value })} required maxLength={300} />
            </div>
            <div>
              <label htmlFor="valorTotal">Valor total (R$)</label>
              <input id="valorTotal" type="number" min="0" step="0.01" value={form.valorTotal} onChange={(e) => setForm({ ...form, valorTotal: e.target.value })} required disabled={!precisaVinculos} />
            </div>
            <div>
              <label htmlFor="duracaoMeses">Duração (meses)</label>
              <input id="duracaoMeses" type="number" min="1" step="1" value={form.duracaoMeses} onChange={(e) => setForm({ ...form, duracaoMeses: e.target.value })} required disabled={!precisaVinculos} />
            </div>
            <div>
              <label htmlFor="dataInicio">Início</label>
              <input id="dataInicio" type="date" value={form.dataInicio} onChange={(e) => setForm({ ...form, dataInicio: e.target.value })} required />
            </div>

            {precisaVinculos && (
              <>
                <div>
                  <label htmlFor="dotacaoId">Dotação orçamentária</label>
                  <select id="dotacaoId" value={form.dotacaoId} onChange={(e) => setForm({ ...form, dotacaoId: e.target.value })} required>
                    <option value="">Selecione…</option>
                    {dotacoes.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.codigo} — saldo {formatarMoeda(d.saldoAtual)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="fornecedorId">Fornecedor</label>
                  <select id="fornecedorId" value={form.fornecedorId} onChange={(e) => setForm({ ...form, fornecedorId: e.target.value })} required>
                    <option value="">Selecione…</option>
                    {fornecedores.map((f) => (
                      <option key={f.id} value={f.id}>{f.nome}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="licitacaoId">Licitação (opcional)</label>
                  <select id="licitacaoId" value={form.licitacaoId} onChange={(e) => setForm({ ...form, licitacaoId: e.target.value })}>
                    <option value="">Sem licitação (dispensa/inexigibilidade)</option>
                    {licitacoes.map((l) => (
                      <option key={l.id} value={l.id}>
                        Edital {l.numeroEdital} ({l.status})
                      </option>
                    ))}
                  </select>
                </div>
              </>
            )}

            <div className="acoes-form">
              <button className="btn primario" type="submit">{editandoId === null ? 'Criar' : 'Salvar'}</button>
              {editandoId !== null && (
                <button className="btn secundario" type="button" onClick={cancelar}>Cancelar</button>
              )}
            </div>
            {!precisaVinculos && (
              <p className="dica campo-largo">
                Valor total, duração e vínculos não podem ser alterados após a criação do contrato.
              </p>
            )}
          </form>
        </div>
      )}

      <TabelaGenerica
        colunas={colunas}
        itens={itens}
        carregando={carregando}
        mensagemVazio="Nenhum contrato cadastrado."
        acoes={
          ehAdmin
            ? (c) => (
                <>
                  <button className="btn secundario" onClick={() => iniciarEdicao(c)}>Editar</button>
                  <button className="btn perigo" onClick={() => excluir(c.id)}>Excluir</button>
                </>
              )
            : undefined
        }
      />
    </section>
  )
}
