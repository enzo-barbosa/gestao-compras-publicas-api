import { useEffect, useState } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { useCrudPage } from '../hooks/useCrudPage'
import { useToast } from '../context/useToast'
import { formatarData, formatarMoeda } from '../utils/format'

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

interface ContratoForm {
  numero: string
  objeto: string
  valorTotal: string
  duracaoMeses: string
  dataInicio: string
  dotacaoId: string
  licitacaoId: string
  fornecedorId: string
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

const FORM_VAZIO: ContratoForm = {
  numero: '',
  objeto: '',
  valorTotal: '',
  duracaoMeses: '12',
  dataInicio: new Date().toISOString().slice(0, 10),
  dotacaoId: '',
  licitacaoId: '',
  fornecedorId: '',
}

const PARAMS = { size: 100, sort: 'dataInicio,desc' } as const

export default function ContratosPage() {
  const { ehAdmin } = useAuth()
  const { exibir } = useToast()
  const [dotacoes, setDotacoes] = useState<DotacaoOpcao[]>([])
  const [fornecedores, setFornecedores] = useState<FornecedorOpcao[]>([])
  const [licitacoes, setLicitacoes] = useState<LicitacaoOpcao[]>([])

  const crud = useCrudPage<Contrato, ContratoForm>({
    rota: '/contratos',
    params: PARAMS,
    formVazio: FORM_VAZIO,
    paraForm: (c) => ({
      numero: c.numero,
      objeto: c.objeto,
      valorTotal: String(c.valorTotal),
      duracaoMeses: String(c.duracaoMeses),
      dataInicio: c.dataInicio,
      dotacaoId: String(c.dotacaoId),
      licitacaoId: c.licitacaoId === null ? '' : String(c.licitacaoId),
      fornecedorId: String(c.fornecedorId),
    }),
    montarCorpo: (form, editandoId) => {
      if (!form.numero.trim()) throw new Error('Informe o número do contrato.')
      if (!form.objeto.trim()) throw new Error('Informe o objeto do contrato.')
      if (editandoId === null) {
        if (Number(form.valorTotal) <= 0) throw new Error('Informe um valor total maior que zero.')
        if (Number(form.duracaoMeses) < 1) throw new Error('A duração mínima é de 1 mês.')
        if (!form.dotacaoId) throw new Error('Selecione a dotação orçamentária.')
        if (!form.fornecedorId) throw new Error('Selecione o fornecedor.')
      }
      return {
        numero: form.numero,
        objeto: form.objeto,
        valorTotal: Number(form.valorTotal),
        duracaoMeses: Number(form.duracaoMeses),
        dataInicio: form.dataInicio,
        dotacaoId: Number(form.dotacaoId),
        licitacaoId: form.licitacaoId ? Number(form.licitacaoId) : null,
        fornecedorId: Number(form.fornecedorId),
      }
    },
    aoSalvar: async (form, editandoId, corpo) => {
      if (editandoId === null) {
        exibir('sucesso', 'Contrato criado — saldo restante igual ao valor total.')
        return api.post('/contratos', corpo)
      }
      const existente = (await api.get<Contrato>(`/contratos/${editandoId}`)).data
      exibir('sucesso', 'Contrato atualizado.')
      return api.put(`/contratos/${editandoId}`, {
        numero: form.numero,
        objeto: form.objeto,
        valorTotal: existente.valorTotal,
        duracaoMeses: existente.duracaoMeses,
        dataInicio: form.dataInicio,
        dotacaoId: existente.dotacaoId,
        licitacaoId: existente.licitacaoId,
        fornecedorId: existente.fornecedorId,
      })
    },
    confirmarExclusao: (c) => `Confirma a exclusão do contrato ${c.numero}?`,
    mensagemCriacao: 'Contrato criado.',
    mensagemEdicao: 'Contrato atualizado.',
    mensagemExclusao: 'Contrato removido.',
  })

  useEffect(() => {
    if (!ehAdmin) return
    api.get<Pagina<DotacaoOpcao>>('/dotacoes', { params: { size: 200 } })
      .then((r) => setDotacoes(r.data.content))
      .catch(() => undefined)
    api.get<Pagina<FornecedorOpcao>>('/fornecedores', { params: { size: 200 } })
      .then((r) => setFornecedores(r.data.content))
      .catch(() => undefined)
    api.get<Pagina<LicitacaoOpcao>>('/licitacoes', { params: { size: 200 } })
      .then((r) => setLicitacoes(r.data.content))
      .catch(() => undefined)
  }, [ehAdmin])

  const colunas: Coluna<Contrato>[] = [
    { key: 'numero', label: 'Número' },
    { key: 'fornecedorNome', label: 'Fornecedor' },
    { key: 'dotacaoCodigo', label: 'Dotação' },
    { key: 'licitacaoNumeroEdital', label: 'Licitação', render: (c) => c.licitacaoNumeroEdital ?? '—' },
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

  const precisaVinculos = crud.editandoId === null

  return (
    <section>
      <h2>Contratos</h2>
      {crud.erro && <div className="alerta erro" role="alert">{crud.erro}</div>}

      {ehAdmin && (
        <div className="card form-card">
          <h3>{crud.editandoId === null ? 'Novo contrato' : `Editando contrato #${crud.editandoId}`}</h3>
          <form onSubmit={crud.salvar} className="grade-form" noValidate>
            <div>
              <label htmlFor="numero">Número</label>
              <input id="numero" value={crud.form.numero} onChange={(e) => crud.setForm({ ...crud.form, numero: e.target.value })} placeholder="015/2026" required maxLength={30} />
            </div>
            <div className="campo-largo">
              <label htmlFor="objeto">Objeto</label>
              <input id="objeto" value={crud.form.objeto} onChange={(e) => crud.setForm({ ...crud.form, objeto: e.target.value })} required maxLength={300} />
            </div>
            <div>
              <label htmlFor="valorTotal">Valor total (R$)</label>
              <input id="valorTotal" type="number" min="0" step="0.01" value={crud.form.valorTotal} onChange={(e) => crud.setForm({ ...crud.form, valorTotal: e.target.value })} required disabled={!precisaVinculos} />
            </div>
            <div>
              <label htmlFor="duracaoMeses">Duração (meses)</label>
              <input id="duracaoMeses" type="number" min="1" step="1" value={crud.form.duracaoMeses} onChange={(e) => crud.setForm({ ...crud.form, duracaoMeses: e.target.value })} required disabled={!precisaVinculos} />
            </div>
            <div>
              <label htmlFor="dataInicio">Início</label>
              <input id="dataInicio" type="date" value={crud.form.dataInicio} onChange={(e) => crud.setForm({ ...crud.form, dataInicio: e.target.value })} required />
            </div>

            {precisaVinculos && (
              <>
                <div>
                  <label htmlFor="dotacaoId">Dotação orçamentária</label>
                  <select id="dotacaoId" value={crud.form.dotacaoId} onChange={(e) => crud.setForm({ ...crud.form, dotacaoId: e.target.value })} required>
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
                  <select id="fornecedorId" value={crud.form.fornecedorId} onChange={(e) => crud.setForm({ ...crud.form, fornecedorId: e.target.value })} required>
                    <option value="">Selecione…</option>
                    {fornecedores.map((f) => (
                      <option key={f.id} value={f.id}>{f.nome}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="licitacaoId">Licitação (opcional)</label>
                  <select id="licitacaoId" value={crud.form.licitacaoId} onChange={(e) => crud.setForm({ ...crud.form, licitacaoId: e.target.value })}>
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
              <button className="btn primario" type="submit">{crud.editandoId === null ? 'Criar' : 'Salvar'}</button>
              {crud.editandoId !== null && (
                <button className="btn secundario" type="button" onClick={crud.cancelar}>Cancelar</button>
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
        itens={crud.itens}
        carregando={crud.carregando}
        mensagemVazio="Nenhum contrato cadastrado."
        ariaLabel="Tabela de contratos"
        acoes={
          ehAdmin
            ? (c) => (
                <>
                  <button className="btn secundario" onClick={() => crud.iniciarEdicao(c)} aria-label={`Editar contrato ${c.numero}`}>Editar</button>
                  <button className="btn perigo" onClick={() => crud.excluir(c)} aria-label={`Excluir contrato ${c.numero}`}>Excluir</button>
                </>
              )
            : undefined
        }
      />
    </section>
  )
}