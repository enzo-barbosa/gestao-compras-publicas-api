import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { useCrudPage } from '../hooks/useCrudPage'
import { formatarMoeda } from '../utils/format'

interface Dotacao {
  id: number
  codigo: string
  descricao: string
  saldoInicial: number
  saldoAtual: number
  anoExercicio: number
}

interface DotacaoForm {
  codigo: string
  descricao: string
  saldoInicial: string
  anoExercicio: string
}

const ANO_ATUAL = new Date().getFullYear()

function formVazio(): DotacaoForm {
  return { codigo: '', descricao: '', saldoInicial: '', anoExercicio: String(ANO_ATUAL) }
}

const PARAMS = { size: 100 }

export default function DotacoesPage() {
  const { ehAdmin } = useAuth()
  const crud = useCrudPage<Dotacao, DotacaoForm>({
    rota: '/dotacoes',
    params: PARAMS,
    formVazio: formVazio(),
    paraForm: (d) => ({
      codigo: d.codigo,
      descricao: d.descricao,
      saldoInicial: String(d.saldoInicial),
      anoExercicio: String(d.anoExercicio),
    }),
    montarCorpo: (form, editandoId) => {
      if (editandoId === null && Number(form.saldoInicial) <= 0) {
        throw new Error('Informe um saldo inicial maior que zero.')
      }
      if (!form.anoExercicio || Number(form.anoExercicio) < 2000) {
        throw new Error('Informe um ano de exercício válido.')
      }
      return {
        codigo: form.codigo,
        descricao: form.descricao,
        saldoInicial: Number(form.saldoInicial),
        anoExercicio: Number(form.anoExercicio),
      }
    },
    confirmarExclusao: (d) => `Confirma a exclusão da dotação ${d.codigo}?`,
    mensagemCriacao: 'Dotação criada.',
    mensagemEdicao: 'Dotação atualizada.',
    mensagemExclusao: 'Dotação removida.',
  })

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
      {crud.erro && <div className="alerta erro" role="alert">{crud.erro}</div>}

      {ehAdmin && (
        <div className="card form-card">
          <h3>{crud.editandoId === null ? 'Nova dotação' : `Editando dotação #${crud.editandoId}`}</h3>
          <form onSubmit={crud.salvar} className="grade-form" noValidate>
            <div>
              <label htmlFor="codigo">Código</label>
              <input id="codigo" value={crud.form.codigo} onChange={(e) => crud.setForm({ ...crud.form, codigo: e.target.value })} required maxLength={30} />
            </div>
            <div>
              <label htmlFor="descricao">Descrição</label>
              <input id="descricao" value={crud.form.descricao} onChange={(e) => crud.setForm({ ...crud.form, descricao: e.target.value })} required maxLength={200} />
            </div>
            <div>
              <label htmlFor="saldoInicial">Saldo inicial (R$)</label>
              <input id="saldoInicial" type="number" min="0" step="0.01" value={crud.form.saldoInicial} onChange={(e) => crud.setForm({ ...crud.form, saldoInicial: e.target.value })} required />
            </div>
            <div>
              <label htmlFor="anoExercicio">Ano exercício</label>
              <input id="anoExercicio" type="number" min="2000" max="2100" value={crud.form.anoExercicio} onChange={(e) => crud.setForm({ ...crud.form, anoExercicio: e.target.value })} required />
            </div>
            <div className="acoes-form">
              <button className="btn primario" type="submit">{crud.editandoId === null ? 'Criar' : 'Salvar'}</button>
              {crud.editandoId !== null && (
                <button className="btn secundario" type="button" onClick={crud.cancelar}>Cancelar</button>
              )}
            </div>
          </form>
        </div>
      )}

      <TabelaGenerica
        colunas={colunas}
        itens={crud.itens}
        carregando={crud.carregando}
        mensagemVazio="Nenhuma dotação cadastrada."
        ariaLabel="Tabela de dotações orçamentárias"
        acoes={
          ehAdmin
            ? (d) => (
                <>
                  <button className="btn secundario" onClick={() => crud.iniciarEdicao(d)} aria-label={`Editar dotação ${d.codigo}`}>Editar</button>
                  <button className="btn perigo" onClick={() => crud.excluir(d)} aria-label={`Excluir dotação ${d.codigo}`}>Excluir</button>
                </>
              )
            : undefined
        }
      />
    </section>
  )
}