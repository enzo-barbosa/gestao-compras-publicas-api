import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/useAuth'
import TabelaGenerica from '../components/TabelaGenerica'
import type { Coluna } from '../components/TabelaGenerica'
import ModalConfirmacao from '../components/ModalConfirmacao'
import { useCrudPage } from '../hooks/useCrudPage'
import { cnpjValido } from '../utils/validacao'
import { mascaraCnpj } from '../utils/format'
import { paramsListagem } from '../utils/listagem'

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

export default function FornecedoresPage() {
  const { podeOperar } = useAuth()
  const [filtroNome, setFiltroNome] = useState('')
  const [filtroNomeAplicado, setFiltroNomeAplicado] = useState('')

  useEffect(() => {
    const timer = setTimeout(() => setFiltroNomeAplicado(filtroNome.trim()), 300)
    return () => clearTimeout(timer)
  }, [filtroNome])

  const params = useMemo(
    () => ({
      ...paramsListagem(),
      ...(filtroNomeAplicado ? { nome: filtroNomeAplicado } : {}),
    }),
    [filtroNomeAplicado],
  )

  const crud = useCrudPage<Fornecedor, FornecedorForm>({
    rota: '/fornecedores',
    params,
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
        throw new Error('CNPJ inválido: verifique os números informados.')
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

      <div className="barra-filtros">
        <input
          type="search"
          aria-label="Buscar fornecedor por nome"
          placeholder="Buscar por nome…"
          value={filtroNome}
          onChange={(e) => setFiltroNome(e.target.value)}
          style={{ flex: 1, minWidth: 200 }}
        />
      </div>

      {podeOperar && (
        <div className="card form-card">
          <h3>{crud.editandoId === null ? 'Novo fornecedor' : `Editando fornecedor #${crud.editandoId}`}</h3>
          <form onSubmit={crud.salvar} className="grade-form" noValidate>
            <div>
              <label htmlFor="nome">Nome / Razão social *</label>
              <input id="nome" value={crud.form.nome} onChange={(e) => crud.setForm({ ...crud.form, nome: e.target.value })} required maxLength={150} />
            </div>
            <div>
              <label htmlFor="cnpj">CNPJ *</label>
              <input id="cnpj" inputMode="numeric" maxLength={18} value={crud.form.cnpj} onChange={(e) => crud.setForm({ ...crud.form, cnpj: mascaraCnpj(e.target.value) })} placeholder="00.000.000/0000-00" required />
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
          podeOperar
            ? (f) => (
                <>
                  <button className="btn secundario" onClick={() => crud.iniciarEdicao(f)} aria-label={`Editar fornecedor ${f.nome}`}>Editar</button>
                  <button className="btn perigo" onClick={() => crud.excluir(f)} aria-label={`Excluir fornecedor ${f.nome}`}>Excluir</button>
                </>
              )
            : undefined
        }
      />
      <ModalConfirmacao
        aberto={crud.exclusao !== null}
        titulo="Excluir fornecedor"
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