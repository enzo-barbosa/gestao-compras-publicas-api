import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { extrairMensagemErro, formatarData, formatarMoeda } from '../utils/format'

interface Licitacao {
  id: number
  numeroEdital: string
  modalidade: string
  objeto: string
  dataAbertura: string
  dataEncerramento: string | null
  status: string
  valorEstimado: number
  vencedor: { id: number; nome: string; cnpj: string } | null
}

interface FornecedorOpcao {
  id: number
  nome: string
}

const MODALIDADES = [
  ['PREGAO', 'Pregão'],
  ['CONCORRENCIA', 'Concorrência'],
  ['TOMADA_DE_PRECOS', 'Tomada de Preços'],
  ['CONVITE', 'Convite'],
  ['CONCURSO', 'Concurso'],
  ['LEILAO', 'Leilão'],
  ['DIALOGO_COMPETITIVO', 'Diálogo Competitivo'],
  ['DISPENSA', 'Dispensa'],
  ['INEXIGIBILIDADE', 'Inexigibilidade'],
] as const

const FORM_VAZIO = {
  numeroEdital: '',
  modalidade: 'PREGAO',
  objeto: '',
  dataAbertura: new Date().toISOString().slice(0, 10),
  dataEncerramento: '',
  valorEstimado: '',
}

export default function LicitacoesPage() {
  const { ehAdmin } = useAuth()
  const [itens, setItens] = useState<Licitacao[]>([])
  const [fornecedores, setFornecedores] = useState<FornecedorOpcao[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)
  const [form, setForm] = useState(FORM_VAZIO)
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [vencedorEm, setVencedorEm] = useState<number | null>(null)
  const [vencedorSelecionado, setVencedorSelecionado] = useState('')

  function carregar() {
    return api
      .get('/licitacoes', { params: { size: 100, sort: 'dataAbertura,desc' } })
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
    if (ehAdmin) {
      api.get('/fornecedores', { params: { size: 200 } })
        .then((r) => setFornecedores(r.data.content ?? []))
        .catch(() => undefined)
    }
  }, [ehAdmin])

  function iniciarEdicao(l: Licitacao) {
    setEditandoId(l.id)
    setForm({
      numeroEdital: l.numeroEdital,
      modalidade: l.modalidade,
      objeto: l.objeto,
      dataAbertura: l.dataAbertura,
      dataEncerramento: l.dataEncerramento ?? '',
      valorEstimado: String(l.valorEstimado),
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
    const corpo = {
      numeroEdital: form.numeroEdital,
      modalidade: form.modalidade,
      objeto: form.objeto,
      dataAbertura: form.dataAbertura,
      dataEncerramento: form.dataEncerramento || null,
      valorEstimado: Number(form.valorEstimado),
    }
    try {
      if (editandoId === null) {
        await api.post('/licitacoes', corpo)
        setSucesso('Licitação criada com sucesso — status ABERTA.')
      } else {
        await api.put(`/licitacoes/${editandoId}`, corpo)
        setSucesso('Licitação atualizada com sucesso.')
      }
      cancelar()
      setCarregando(true)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function excluir(id: number) {
    if (!window.confirm('Confirma a exclusão desta licitação?')) return
    setErro(null)
    setSucesso(null)
    try {
      await api.delete(`/licitacoes/${id}`)
      setSucesso('Licitação removida.')
      setCarregando(true)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function definirVencedor(id: number) {
    if (!vencedorSelecionado) return
    setErro(null)
    setSucesso(null)
    try {
      await api.put(`/licitacoes/${id}/vencedor`, { fornecedorId: Number(vencedorSelecionado) })
      setSucesso('Vencedor definido — licitação encerrada.')
      setVencedorEm(null)
      setVencedorSelecionado('')
      setCarregando(true)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  const colunas: Coluna<Licitacao>[] = [
    { key: 'numeroEdital', label: 'Edital' },
    {
      key: 'modalidade',
      label: 'Modalidade',
      render: (l) => MODALIDADES.find(([valor]) => valor === l.modalidade)?.[1] ?? l.modalidade,
    },
    {
      key: 'objeto',
      label: 'Objeto',
      render: (l) => <span className="objeto">{l.objeto}</span>,
    },
    { key: 'dataAbertura', label: 'Abertura', render: (l) => formatarData(l.dataAbertura) },
    {
      key: 'valorEstimado',
      label: 'Valor estimado',
      render: (l) => formatarMoeda(l.valorEstimado),
    },
    {
      key: 'status',
      label: 'Status',
      render: (l) => (
        <span className={`badge status-${l.status.toLowerCase()}`}>
          {l.status.charAt(0) + l.status.slice(1).toLowerCase()}
        </span>
      ),
    },
    {
      key: 'vencedor',
      label: 'Vencedor',
      render: (l) => l.vencedor?.nome ?? '—',
    },
  ]

  return (
    <section>
      <h2>Licitações</h2>
      {erro && <div className="alerta erro">{erro}</div>}
      {sucesso && <div className="alerta sucesso">{sucesso}</div>}

      {ehAdmin && (
        <>
          <div className="card form-card">
            <h3>{editandoId === null ? 'Nova licitação' : `Editando licitação #${editandoId}`}</h3>
            <form onSubmit={salvar} className="grade-form">
              <div>
                <label htmlFor="numeroEdital">Número do edital</label>
                <input id="numeroEdital" value={form.numeroEdital} onChange={(e) => setForm({ ...form, numeroEdital: e.target.value })} placeholder="001/2026" required maxLength={30} />
              </div>
              <div>
                <label htmlFor="modalidade">Modalidade</label>
                <select id="modalidade" value={form.modalidade} onChange={(e) => setForm({ ...form, modalidade: e.target.value })}>
                  {MODALIDADES.map(([valor, rotulo]) => (
                    <option key={valor} value={valor}>{rotulo}</option>
                  ))}
                </select>
              </div>
              <div className="campo-largo">
                <label htmlFor="objeto">Objeto</label>
                <input id="objeto" value={form.objeto} onChange={(e) => setForm({ ...form, objeto: e.target.value })} required maxLength={300} />
              </div>
              <div>
                <label htmlFor="dataAbertura">Abertura</label>
                <input id="dataAbertura" type="date" value={form.dataAbertura} onChange={(e) => setForm({ ...form, dataAbertura: e.target.value })} required />
              </div>
              <div>
                <label htmlFor="dataEncerramento">Encerramento</label>
                <input id="dataEncerramento" type="date" value={form.dataEncerramento} onChange={(e) => setForm({ ...form, dataEncerramento: e.target.value })} />
              </div>
              <div>
                <label htmlFor="valorEstimado">Valor estimado (R$)</label>
                <input id="valorEstimado" type="number" min="0" step="0.01" value={form.valorEstimado} onChange={(e) => setForm({ ...form, valorEstimado: e.target.value })} required />
              </div>
              <div className="acoes-form">
                <button className="btn primario" type="submit">{editandoId === null ? 'Criar' : 'Salvar'}</button>
                {editandoId !== null && (
                  <button className="btn secundario" type="button" onClick={cancelar}>Cancelar</button>
                )}
              </div>
            </form>
          </div>

          {vencedorEm !== null && (
            <div className="card form-card destaque">
              <h3>Definir vencedor da licitação #{vencedorEm}</h3>
              <div className="linha-vencedor">
                <select value={vencedorSelecionado} onChange={(e) => setVencedorSelecionado(e.target.value)}>
                  <option value="">Selecione o fornecedor vencedor…</option>
                  {fornecedores.map((f) => (
                    <option key={f.id} value={f.id}>{f.nome}</option>
                  ))}
                </select>
                <button className="btn primario" disabled={!vencedorSelecionado} onClick={() => definirVencedor(vencedorEm)}>
                  Confirmar encerramento
                </button>
                <button className="btn secundario" onClick={() => setVencedorEm(null)}>Cancelar</button>
              </div>
              <p className="dica">O vencedor é registrado, a licitação fica ENCERRADA e o contrato poderá vinculá-la.</p>
            </div>
          )}
        </>
      )}

      <TabelaGenerica
        colunas={colunas}
        itens={itens}
        carregando={carregando}
        mensagemVazio="Nenhuma licitação cadastrada."
        acoes={
          ehAdmin
            ? (l) => (
                <>
                  {l.status === 'ABERTA' && (
                    <button className="btn secundario" onClick={() => iniciarEdicao(l)}>Editar</button>
                  )}
                  {(l.status === 'ABERTA' || l.status === 'ENCERRADA') && (
                    <button
                      className="btn primario"
                      onClick={() => { setVencedorEm(l.id); setVencedorSelecionado(String(l.vencedor?.id ?? '')) }}
                    >
                      Vencedor
                    </button>
                  )}
                  {l.status === 'ABERTA' && !l.vencedor && (
                    <button className="btn perigo" onClick={() => excluir(l.id)}>Excluir</button>
                  )}
                </>
              )
            : undefined
        }
      />
    </section>
  )
}
