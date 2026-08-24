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
}

export default function TabelaGenerica<T extends { id: number }>({
  colunas,
  itens,
  carregando = false,
  mensagemVazio = 'Nenhum registro encontrado.',
  acoes,
}: Props<T>) {
  if (carregando) {
    return <p className="vazio">Carregando…</p>
  }

  if (itens.length === 0) {
    return <p className="vazio">{mensagemVazio}</p>
  }

  return (
    <table className="tabela">
      <thead>
        <tr>
          {colunas.map((c) => (
            <th key={c.key}>{c.label}</th>
          ))}
          {acoes && <th>Ações</th>}
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
