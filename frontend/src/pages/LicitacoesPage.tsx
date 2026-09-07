import { useEffect, useState } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { useCrudPage } from '../hooks/useCrudPage'
import { useToast } from '../context/useToast'
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

interface LicitacaoForm {
  numeroEdital: string
  modalidade: string
  objeto: string
  dataAbertura: string
  dataEncerramento: string
  valorEstimado: string
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

const FORM_VAZIO: LicitacaoForm = {
  numeroEdital: '',
  modalidade: 'PREGAO',
  objeto: '',
  dataAbertura: new Date().toISOString().slice(0, 10),
  dataEncerramento: '',
  valorEstimado: '',
}

const PARAMS = { size: 100, sort: 'dataAbertura,desc' } as const

export default function LicitacoesPage() {
  const { ehAdmin } = useAuth()
  const { exibir } = useToast()
  const [fornecedores, setFornecedores] = useState<FornecedorOpcao[]>([])
  const [vencedorEm, setVencedorEm] = useState<number | null>(null)
  const [vencedorSelecionado, setVencedorSelecionado] = useState('')

  const crud = useCrudPage<Licitacao, LicitacaoForm>({
    rota: '/licitacoes',
    params: PARAMS,
    formVazio: FORM_VAZIO,
    paraForm: (l) => ({
      numeroEdital: l.numeroEdital,
      modalidade: l.modalidade,
      objeto: l.objeto,
      dataAbertura: l.dataAbertura,
      dataEncerramento: l.dataEncerramento ?? '',
      valorEstimado: String(l.valorEstimado),
    }),
    montarCorpo: (form) => {
      if (Number(form.valorEstimado) <= 0) {
        throw new Error('Informe um valor estimado maior que zero.')
      }
      if (form.dataEncerramento && form.dataEncerramento < form.dataAbertura) {
        throw new Error('O encerramento não pode ser anterior à abertura.')
      }
      return {
        numeroEdital: form.numeroEdital,
        modalidade: form.modalidade,
        objeto: form.objeto,
        dataAbertura: form.dataAbertura,
        dataEncerramento: form.dataEncerramento || null,
        valorEstimado: Number(form.valorEstimado),
      }
    },
    confirmarExclusao: (l) => `Confirma a exclusão da licitação ${l.numeroEdital}?`,
    mensagemCriacao: 'Licitação criada.',
    mensagemEdicao: 'Licitação atualizada.',
    mensagemExclusao: 'Licitação removida.',
  })

  useEffect(() => {
    if (!ehAdmin) return
    api.get<Pagina<FornecedorOpcao>>('/fornecedores', { params: { size: 200 } })
      .then((r) => setFornecedores(r.data.content))
      .catch(() => undefined)
  }, [ehAdmin])

  async function definirVencedor(id: number) {
    if (!vencedorSelecionado) return
    crud.setErro(null)
    try {
      await api.put(`/licitacoes/${id}/vencedor`, { fornecedorId: Number(vencedorSelecionado) })
      exibir('sucesso', 'Licitação encerrada com vencedor definido.')
      setVencedorEm(null)
      setVencedorSelecionado('')
      await crud.carregar()
    } catch (e) {
      crud.setErro(extrairMensagemErro(e))
    }
  }

  const colunas: Coluna<Licitacao>[] = [
    { key: 'numeroEdital', label: 'Edital' },
    {
      key: 'modalidade',
      label: 'Modalidade',
      render: (l) => MODALIDADES.find(([valor]) => valor === l.modalidade)?.[1] ?? l.modalidade,
    },
    { key: 'objeto', label: 'Objeto', render: (l) => <span className="objeto">{l.objeto}</span> },
    { key: 'dataAbertura', label: 'Abertura', render: (l) => formatarData(l.dataAbertura) },
    { key: 'valorEstimado', label: 'Valor estimado', render: (l) => formatarMoeda(l.valorEstimado) },
    {
      key: 'status',
      label: 'Status',
      render: (l) => (
        <span className={`badge status-${l.status.toLowerCase()}`}>
          {l.status.charAt(0) + l.status.slice(1).toLowerCase()}
        </span>
      ),
    },
    { key: 'vencedor', label: 'Vencedor', render: (l) => l.vencedor?.nome ?? '—' },
  ]

  return (
    <section>
      <h2>Licitações</h2>
      {crud.erro && <div className="alerta erro" role="alert">{crud.erro}</div>}

      {ehAdmin && (
        <>
          <div className="card form-card">
            <h3>{crud.editandoId === null ? 'Nova licitação' : `Editando licitação #${crud.editandoId}`}</h3>
            <form onSubmit={crud.salvar} className="grade-form" noValidate>
              <div>
                <label htmlFor="numeroEdital">Número do edital</label>
                <input id="numeroEdital" value={crud.form.numeroEdital} onChange={(e) => crud.setForm({ ...crud.form, numeroEdital: e.target.value })} placeholder="001/2026" required maxLength={30} />
              </div>
              <div>
                <label htmlFor="modalidade">Modalidade</label>
                <select id="modalidade" value={crud.form.modalidade} onChange={(e) => crud.setForm({ ...crud.form, modalidade: e.target.value })}>
                  {MODALIDADES.map(([valor, rotulo]) => (
                    <option key={valor} value={valor}>{rotulo}</option>
                  ))}
                </select>
              </div>
              <div className="campo-largo">
                <label htmlFor="objeto">Objeto</label>
                <input id="objeto" value={crud.form.objeto} onChange={(e) => crud.setForm({ ...crud.form, objeto: e.target.value })} required maxLength={300} />
              </div>
              <div>
                <label htmlFor="dataAbertura">Abertura</label>
                <input id="dataAbertura" type="date" value={crud.form.dataAbertura} onChange={(e) => crud.setForm({ ...crud.form, dataAbertura: e.target.value })} required />
              </div>
              <div>
                <label htmlFor="dataEncerramento">Encerramento</label>
                <input id="dataEncerramento" type="date" value={crud.form.dataEncerramento} onChange={(e) => crud.setForm({ ...crud.form, dataEncerramento: e.target.value })} />
              </div>
              <div>
                <label htmlFor="valorEstimado">Valor estimado (R$)</label>
                <input id="valorEstimado" type="number" min="0" step="0.01" value={crud.form.valorEstimado} onChange={(e) => crud.setForm({ ...crud.form, valorEstimado: e.target.value })} required />
              </div>
              <div className="acoes-form">
                <button className="btn primario" type="submit">{crud.editandoId === null ? 'Criar' : 'Salvar'}</button>
                {crud.editandoId !== null && (
                  <button className="btn secundario" type="button" onClick={crud.cancelar}>Cancelar</button>
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
        itens={crud.itens}
        carregando={crud.carregando}
        mensagemVazio="Nenhuma licitação cadastrada."
        ariaLabel="Tabela de licitações"
        acoes={
          ehAdmin
            ? (l) => (
                <>
                  {l.status === 'ABERTA' && (
                    <button className="btn secundario" onClick={() => crud.iniciarEdicao(l)} aria-label={`Editar licitação ${l.numeroEdital}`}>Editar</button>
                  )}
                  {(l.status === 'ABERTA' || l.status === 'ENCERRADA') && (
                    <button
                      className="btn primario"
                      onClick={() => { setVencedorEm(l.id); setVencedorSelecionado(String(l.vencedor?.id ?? '')) }}
                      aria-label={`Definir vencedor da licitação ${l.numeroEdital}`}
                    >
                      Vencedor
                    </button>
                  )}
                  {l.status === 'ABERTA' && !l.vencedor && (
                    <button className="btn perigo" onClick={() => crud.excluir(l)} aria-label={`Excluir licitação ${l.numeroEdital}`}>Excluir</button>
                  )}
                </>
              )
            : undefined
        }
      />
    </section>
  )
}