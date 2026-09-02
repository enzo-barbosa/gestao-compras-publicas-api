import type { ReactNode } from 'react'

export interface Coluna<T> {
  key: string
  label: string
  render?: (item: T) => ReactNode
}

interface Props<T extends { id: number }> {
  colunas: Coluna<T>[]
  itens: T[]
  carregando?: boolean
  mensagemVazio?: string
  acoes?: (item: T) => ReactNode
  ariaLabel?: string
}

export default function TabelaGenerica<T extends { id: number }>({
  colunas,
  itens,
  carregando = false,
  mensagemVazio = 'Nenhum registro encontrado.',
  acoes,
  ariaLabel = 'Tabela de registros',
}: Props<T>) {
  if (carregando) {
    return (
      <p className="vazio" role="status" aria-busy="true">
        Carregando…
      </p>
    )
  }

  if (itens.length === 0) {
    return (
      <p className="vazio" role="status">
        {mensagemVazio}
      </p>
    )
  }

  return (
    <table className="tabela" aria-label={ariaLabel}>
      <caption className="sr-only">{ariaLabel}</caption>
      <thead>
        <tr>
          {colunas.map((c) => (
            <th key={c.key} scope="col">{c.label}</th>
          ))}
          {acoes && <th scope="col">Ações</th>}
        </tr>
      </thead>
      <tbody>
        {itens.map((item) => (
          <tr key={item.id}>
            {colunas.map((c) => (
              <td key={c.key}>
                {c.render ? c.render(item) : String((item as Record<string, unknown>)[c.key] ?? '—')}
              </td>
            ))}
            {acoes && (
              <td>
                <div className="acoes-linha">{acoes(item)}</div>
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
