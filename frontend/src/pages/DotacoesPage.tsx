import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { extrairMensagemErro, formatarMoeda } from '../utils/format'

interface Dotacao {
  id: number
  codigo: string
  descricao: string
  saldoInicial: number
  saldoAtual: number
  anoExercicio: number
}

const FORM_VAZIO = { codigo: '', descricao: '', saldoInicial: '', anoExercicio: '2026' }

export default function DotacoesPage() {
  const { ehAdmin } = useAuth()
  const [itens, setItens] = useState<Dotacao[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)
  const [form, setForm] = useState(FORM_VAZIO)
  const [editandoId, setEditandoId] = useState<number | null>(null)

  async function carregar() {
    setCarregando(true)
    try {
      const resposta = await api.get('/dotacoes', { params: { size: 100 } })
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
  }, [])

  function iniciarEdicao(d: Dotacao) {
    setEditandoId(d.id)
    setForm({
      codigo: d.codigo,
      descricao: d.descricao,
      saldoInicial: String(d.saldoInicial),
      anoExercicio: String(d.anoExercicio),
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
      codigo: form.codigo,
      descricao: form.descricao,
      saldoInicial: Number(form.saldoInicial),
      anoExercicio: Number(form.anoExercicio),
    }
    try {
      if (editandoId === null) {
        await api.post('/dotacoes', corpo)
        setSucesso('Dotação criada com sucesso.')
      } else {
        await api.put(`/dotacoes/${editandoId}`, corpo)
        setSucesso('Dotação atualizada com sucesso.')
      }
      cancelar()
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function excluir(id: number) {
    if (!window.confirm('Confirma a exclusão desta dotação orçamentária?')) return
    setErro(null)
    setSucesso(null)
    try {
      await api.delete(`/dotacoes/${id}`)
      setSucesso('Dotação removida.')
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  const colunas: Coluna<Dotacao>[] = [
    { key: 'codigo', label: 'Código' },
    { key: 'descricao', label: 'Descrição' },
    { key: 'anoExercicio', label: 'Exercício' },
    {
      key: 'saldoAtual',
      label: 'Saldo atual',
      render: (d) => <strong className={d.saldoAtual <= 0 ? 'texto-vermelho' : 'texto-verde'}>{formatarMoeda(d.saldoAtual)}</strong>,
    },
  ]

  return (
    <section>
      <h2>Dotações orçamentárias</h2>
      {erro && <div className="alerta erro">{erro}</div>}
      {sucesso && <div className="alerta sucesso">{sucesso}</div>}

      {ehAdmin && (
        <div className="card form-card">
          <h3>{editandoId === null ? 'Nova dotação' : `Editando dotação #${editandoId}`}</h3>
          <form onSubmit={salvar} className="grade-form">
            <div>
              <label htmlFor="codigo">Código</label>
              <input id="codigo" value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })} required maxLength={30} />
            </div>
            <div>
              <label htmlFor="descricao">Descrição</label>
              <input id="descricao" value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} required maxLength={200} />
            </div>
            <div>
              <label htmlFor="saldoInicial">Saldo inicial (R$)</label>
              <input id="saldoInicial" type="number" min="0" step="0.01" value={form.saldoInicial} onChange={(e) => setForm({ ...form, saldoInicial: e.target.value })} required />
            </div>
            <div>
              <label htmlFor="anoExercicio">Ano exercício</label>
              <input id="anoExercicio" type="number" min="2000" max="2100" value={form.anoExercicio} onChange={(e) => setForm({ ...form, anoExercicio: e.target.value })} required />
            </div>
            <div className="acoes-form">
              <button className="btn primario" type="submit">{editandoId === null ? 'Criar' : 'Salvar'}</button>
              {editandoId !== null && (
                <button className="btn secundario" type="button" onClick={cancelar}>Cancelar</button>
              )}
            </div>
          </form>
        </div>
      )}

      <TabelaGenerica
        colunas={colunas}
        itens={itens}
        carregando={carregando}
        mensagemVazio="Nenhuma dotação cadastrada."
        acoes={
          ehAdmin
            ? (d) => (
                <>
                  <button className="btn secundario" onClick={() => iniciarEdicao(d)}>Editar</button>
                  <button className="btn perigo" onClick={() => excluir(d.id)}>Excluir</button>
                </>
              )
            : undefined
        }
      />
    </section>
  )
}
