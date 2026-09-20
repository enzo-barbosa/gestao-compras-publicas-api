import { useEffect, useMemo, useState } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import ModalConfirmacao from '../components/ModalConfirmacao'
import { useCrudPage } from '../hooks/useCrudPage'
import { useToast } from '../context/useToast'
import { dataEncerramentoValida, dataFuturaOuHoje, numeroEditalValido } from '../utils/validacao'
import { extrairMensagemErro, formatarData, formatarMoeda, mascaraMoeda, mascaraNumeroEdital, valorDaMascaraMoeda } from '../utils/format'
import { paramsListagem } from '../utils/listagem'

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

const STATUS_LICITACAO = [
  ['ABERTA', 'Aberta'],
  ['ENCERRADA', 'Encerrada'],
  ['HOMOLOGADA', 'Homologada'],
  ['CANCELADA', 'Cancelada'],
] as const

export default function LicitacoesPage() {
  const { podeOperar } = useAuth()
  const { exibir } = useToast()
  const [fornecedores, setFornecedores] = useState<FornecedorOpcao[]>([])
  const [vencedorEm, setVencedorEm] = useState<number | null>(null)
  const [vencedorSelecionado, setVencedorSelecionado] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('')
  const [filtroModalidade, setFiltroModalidade] = useState('')

  const params = useMemo(
    () => ({
      ...paramsListagem('dataAbertura,desc'),
      ...(filtroStatus ? { status: filtroStatus } : {}),
      ...(filtroModalidade ? { modalidade: filtroModalidade } : {}),
    }),
    [filtroStatus, filtroModalidade],
  )

  const crud = useCrudPage<Licitacao, LicitacaoForm>({
    rota: '/licitacoes',
    params,
    formVazio: FORM_VAZIO,
    paraForm: (l) => ({
      numeroEdital: l.numeroEdital,
      modalidade: l.modalidade,
      objeto: l.objeto,
      dataAbertura: l.dataAbertura,
      dataEncerramento: l.dataEncerramento ?? '',
      valorEstimado: mascaraMoeda(l.valorEstimado.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })),
    }),
    montarCorpo: (form) => {
      const valorEstimado = valorDaMascaraMoeda(form.valorEstimado)
      if (valorEstimado <= 0) {
        throw new Error('Informe um valor estimado maior que zero.')
      }
      if (!numeroEditalValido(form.numeroEdital)) {
        throw new Error('Informe o edital no formato NNNN/AAAA, ex.: 001/2026.')
      }
      if (!dataFuturaOuHoje(form.dataAbertura)) {
        throw new Error('A abertura não pode ser anterior à data de hoje.')
      }
      if (!dataEncerramentoValida(form.dataAbertura, form.dataEncerramento)) {
        throw new Error('O encerramento não pode ser anterior à abertura.')
      }
      return {
        numeroEdital: form.numeroEdital,
        modalidade: form.modalidade,
        objeto: form.objeto,
        dataAbertura: form.dataAbertura,
        dataEncerramento: form.dataEncerramento || null,
        valorEstimado,
      }
    },
    confirmarExclusao: (l) => `Confirma a exclusão da licitação ${l.numeroEdital}?`,
    mensagemCriacao: 'Licitação criada.',
    mensagemEdicao: 'Licitação atualizada.',
    mensagemExclusao: 'Licitação removida.',
  })

  useEffect(() => {
    if (!podeOperar) return
    api.get<Pagina<FornecedorOpcao>>('/fornecedores', { params: { size: 200 } })
      .then((r) => setFornecedores(r.data.content))
      .catch(() => undefined)
  }, [podeOperar])

  async function definirVencedor(id: number) {
    const fornecedor = fornecedores.find((f) => f.nome === vencedorSelecionado || String(f.id) === vencedorSelecionado)
    if (!fornecedor) {
      crud.setErro('Selecione um fornecedor da lista de vencedores.')
      return
    }
    crud.setErro(null)
    try {
      await api.put(`/licitacoes/${id}/vencedor`, { fornecedorId: fornecedor.id })
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

      <div className="barra-filtros">
        <select aria-label="Filtrar licitações por status" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)}>
          <option value="">Todos os status</option>
          {STATUS_LICITACAO.map(([valor, rotulo]) => (
            <option key={valor} value={valor}>{rotulo}</option>
          ))}
        </select>
        <select aria-label="Filtrar licitações por modalidade" value={filtroModalidade} onChange={(e) => setFiltroModalidade(e.target.value)}>
          <option value="">Todas as modalidades</option>
          {MODALIDADES.map(([valor, rotulo]) => (
            <option key={valor} value={valor}>{rotulo}</option>
          ))}
        </select>
      </div>

      {podeOperar && (
        <>
          <div className="card form-card">
            <h3>{crud.editandoId === null ? 'Nova licitação' : 'Editando licitação'}</h3>
            <form onSubmit={crud.salvar} className="grade-form" noValidate>
              <div>
                <label htmlFor="numeroEdital">Número do edital *</label>
                <input id="numeroEdital" inputMode="numeric" value={crud.form.numeroEdital} onChange={(e) => crud.setForm({ ...crud.form, numeroEdital: mascaraNumeroEdital(e.target.value) })} placeholder="001/2026" required maxLength={9} />
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
                <label htmlFor="objeto">Objeto *</label>
                <input id="objeto" value={crud.form.objeto} onChange={(e) => crud.setForm({ ...crud.form, objeto: e.target.value })} required maxLength={300} />
              </div>
              <div>
                <label htmlFor="dataAbertura">Abertura *</label>
                <input id="dataAbertura" type="date" value={crud.form.dataAbertura} onChange={(e) => crud.setForm({ ...crud.form, dataAbertura: e.target.value })} required />
              </div>
              <div>
                <label htmlFor="dataEncerramento">Encerramento</label>
                <input id="dataEncerramento" type="date" value={crud.form.dataEncerramento} onChange={(e) => crud.setForm({ ...crud.form, dataEncerramento: e.target.value })} />
              </div>
              <div>
                <label htmlFor="valorEstimado">Valor estimado (R$) *</label>
                <input id="valorEstimado" inputMode="decimal" placeholder="0,00" value={crud.form.valorEstimado} onChange={(e) => crud.setForm({ ...crud.form, valorEstimado: mascaraMoeda(e.target.value) })} required />
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
              <h3>Definir vencedor da licitação</h3>
              <div className="linha-vencedor">
                <input
                  list="lista-fornecedores"
                  value={vencedorSelecionado}
                  onChange={(e) => setVencedorSelecionado(e.target.value)}
                  placeholder="Digite o nome do fornecedor vencedor…"
                />
                <datalist id="lista-fornecedores">
                  {fornecedores.map((f) => (
                    <option key={f.id} value={f.nome} />
                  ))}
                </datalist>
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
          podeOperar
            ? (l) => (
                <>
                  {l.status === 'ABERTA' && (
                    <button className="btn secundario" onClick={() => crud.iniciarEdicao(l)} aria-label={`Editar licitação ${l.numeroEdital}`}>Editar</button>
                  )}
                  {(l.status === 'ABERTA' || l.status === 'ENCERRADA') && (
                    <button
                      className="btn primario"
                      onClick={() => { setVencedorEm(l.id); setVencedorSelecionado(l.vencedor?.nome ?? '') }}
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
      <ModalConfirmacao
        aberto={crud.exclusao !== null}
        titulo="Excluir licitação"
        mensagem={crud.exclusao?.mensagem ?? ''}
        rotuloConfirmar="Excluir"
        rotuloCancelar="Cancelar"
        confirmando={crud.excluindo}
        aoConfirmar={crud.confirmarExclusao}
        aoCancelar={crud.cancelarExclusao}
      />
    </section>
  )
}