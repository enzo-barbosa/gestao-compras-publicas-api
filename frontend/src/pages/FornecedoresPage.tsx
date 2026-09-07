import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import { useCrudPage } from '../hooks/useCrudPage'
import { cnpjValido } from '../utils/validacao'

interface Fornecedor {
  id: number
  nome: string
  cnpj: string
  email: string | null
  telefone: string | null
  endereco: string | null
}

interface FornecedorForm {
  nome: string
  cnpj: string
  email: string
  telefone: string
  endereco: string
}

const FORM_VAZIO: FornecedorForm = { nome: '', cnpj: '', email: '', telefone: '', endereco: '' }

const PARAMS = { size: 100 }

export default function FornecedoresPage() {
  const { ehAdmin } = useAuth()
  const crud = useCrudPage<Fornecedor, FornecedorForm>({
    rota: '/fornecedores',
    params: PARAMS,
    formVazio: FORM_VAZIO,
    paraForm: (f) => ({
      nome: f.nome,
      cnpj: f.cnpj,
      email: f.email ?? '',
      telefone: f.telefone ?? '',
      endereco: f.endereco ?? '',
    }),
    montarCorpo: (form) => {
      const apenasDigitos = form.cnpj.replace(/\D/g, '')
      if (!cnpjValido(form.cnpj)) {
        throw new Error('CNPJ inválido: informe os 14 dígitos.')
      }
      return {
        nome: form.nome,
        cnpj: apenasDigitos,
        email: form.email || null,
        telefone: form.telefone || null,
        endereco: form.endereco || null,
      }
    },
    confirmarExclusao: (f) => `Confirma a exclusão do fornecedor ${f.nome}?`,
    mensagemCriacao: 'Fornecedor criado.',
    mensagemEdicao: 'Fornecedor atualizado.',
    mensagemExclusao: 'Fornecedor removido.',
  })

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
      {crud.erro && <div className="alerta erro" role="alert">{crud.erro}</div>}

      {ehAdmin && (
        <div className="card form-card">
          <h3>{crud.editandoId === null ? 'Novo fornecedor' : `Editando fornecedor #${crud.editandoId}`}</h3>
          <form onSubmit={crud.salvar} className="grade-form" noValidate>
            <div>
              <label htmlFor="nome">Nome / Razão social</label>
              <input id="nome" value={crud.form.nome} onChange={(e) => crud.setForm({ ...crud.form, nome: e.target.value })} required maxLength={150} />
            </div>
            <div>
              <label htmlFor="cnpj">CNPJ</label>
              <input id="cnpj" value={crud.form.cnpj} onChange={(e) => crud.setForm({ ...crud.form, cnpj: e.target.value })} placeholder="00.000.000/0000-00" required />
            </div>
            <div>
              <label htmlFor="email">E-mail</label>
              <input id="email" type="email" value={crud.form.email} onChange={(e) => crud.setForm({ ...crud.form, email: e.target.value })} />
            </div>
            <div>
              <label htmlFor="telefone">Telefone</label>
              <input id="telefone" value={crud.form.telefone} onChange={(e) => crud.setForm({ ...crud.form, telefone: e.target.value })} maxLength={20} />
            </div>
            <div className="campo-largo">
              <label htmlFor="endereco">Endereço</label>
              <input id="endereco" value={crud.form.endereco} onChange={(e) => crud.setForm({ ...crud.form, endereco: e.target.value })} maxLength={200} />
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
        mensagemVazio="Nenhum fornecedor cadastrado."
        ariaLabel="Tabela de fornecedores"
        acoes={
          ehAdmin
            ? (f) => (
                <>
                  <button className="btn secundario" onClick={() => crud.iniciarEdicao(f)} aria-label={`Editar fornecedor ${f.nome}`}>Editar</button>
                  <button className="btn perigo" onClick={() => crud.excluir(f)} aria-label={`Excluir fornecedor ${f.nome}`}>Excluir</button>
                </>
              )
            : undefined
        }
      />
    </section>
  )
}