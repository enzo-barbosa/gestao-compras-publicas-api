import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { extrairMensagemErro } from '../utils/format'

interface Fornecedor {
  id: number
  nome: string
  cnpj: string
  email: string | null
  telefone: string | null
  endereco: string | null
}

const FORM_VAZIO = { nome: '', cnpj: '', email: '', telefone: '', endereco: '' }

export default function FornecedoresPage() {
  const { ehAdmin } = useAuth()
  const [itens, setItens] = useState<Fornecedor[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)
  const [form, setForm] = useState(FORM_VAZIO)
  const [editandoId, setEditandoId] = useState<number | null>(null)

  function carregar() {
    return api
      .get('/fornecedores', { params: { size: 100 } })
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

  function iniciarEdicao(f: Fornecedor) {
    setEditandoId(f.id)
    setForm({
      nome: f.nome,
      cnpj: f.cnpj,
      email: f.email ?? '',
      telefone: f.telefone ?? '',
      endereco: f.endereco ?? '',
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
      nome: form.nome,
      cnpj: form.cnpj,
      email: form.email || null,
      telefone: form.telefone || null,
      endereco: form.endereco || null,
    }
    try {
      if (editandoId === null) {
        await api.post('/fornecedores', corpo)
        setSucesso('Fornecedor criado com sucesso.')
      } else {
        await api.put(`/fornecedores/${editandoId}`, corpo)
        setSucesso('Fornecedor atualizado com sucesso.')
      }
      cancelar()
      setCarregando(true)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function excluir(id: number) {
    if (!window.confirm('Confirma a exclusão deste fornecedor?')) return
    setErro(null)
    setSucesso(null)
    try {
      await api.delete(`/fornecedores/${id}`)
      setSucesso('Fornecedor removido.')
      setCarregando(true)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  const colunas: Coluna<Fornecedor>[] = [
    { key: 'nome', label: 'Nome' },
    {
      key: 'cnpj',
      label: 'CNPJ',
      render: (f) => (
        <span className="mono">
          {f.cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')}
        </span>
      ),
    },
    { key: 'email', label: 'E-mail' },
    { key: 'telefone', label: 'Telefone' },
  ]

  return (
    <section>
      <h2>Fornecedores</h2>
      {erro && <div className="alerta erro" role="alert">{erro}</div>}
      {sucesso && <div className="alerta sucesso" role="status">{sucesso}</div>}

      {ehAdmin && (
        <div className="card form-card">
          <h3>{editandoId === null ? 'Novo fornecedor' : `Editando fornecedor #${editandoId}`}</h3>
          <form onSubmit={salvar} className="grade-form">
            <div>
              <label htmlFor="nome">Nome / Razão social</label>
              <input id="nome" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required maxLength={150} />
            </div>
            <div>
              <label htmlFor="cnpj">CNPJ</label>
              <input id="cnpj" value={form.cnpj} onChange={(e) => setForm({ ...form, cnpj: e.target.value })} placeholder="00.000.000/0000-00" required />
            </div>
            <div>
              <label htmlFor="email">E-mail</label>
              <input id="email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div>
              <label htmlFor="telefone">Telefone</label>
              <input id="telefone" value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} maxLength={20} />
            </div>
            <div className="campo-largo">
              <label htmlFor="endereco">Endereço</label>
              <input id="endereco" value={form.endereco} onChange={(e) => setForm({ ...form, endereco: e.target.value })} maxLength={200} />
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
        mensagemVazio="Nenhum fornecedor cadastrado."
        ariaLabel="Tabela de fornecedores"
        acoes={
          ehAdmin
            ? (f) => (
                <>
                  <button className="btn secundario" onClick={() => iniciarEdicao(f)} aria-label={`Editar fornecedor ${f.nome}`}>Editar</button>
                  <button className="btn perigo" onClick={() => excluir(f.id)} aria-label={`Excluir fornecedor ${f.nome}`}>Excluir</button>
                </>
              )
            : undefined
        }
      />
    </section>
  )
}
