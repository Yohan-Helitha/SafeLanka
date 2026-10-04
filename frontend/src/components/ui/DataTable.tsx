import clsx from 'clsx'
import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: ReactNode
  cell: (row: T) => ReactNode
  className?: string
  align?: 'left' | 'right'
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string
  onRowClick?: (row: T) => void
  empty?: ReactNode
  caption: string
  /** Minimum table width in px before it scrolls sideways inside its own container. Pass 0 for small tables. */
  minWidth?: number
}

export function DataTable<T>({ columns, rows, rowKey, onRowClick, empty, caption, minWidth = 640 }: DataTableProps<T>) {
  if (!rows.length && empty) return <>{empty}</>
  return (
    <div className="overflow-x-auto rounded-card border border-line bg-panel">
      <table className="w-full border-collapse text-left text-[15px]" style={{ minWidth: minWidth || undefined }}>
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr className="border-b border-line text-xs uppercase tracking-wide text-faint">
            {columns.map((c) => (
              <th key={c.key} scope="col" className={clsx('px-4 py-2.5 font-medium', c.align === 'right' && 'text-right', c.className)}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              tabIndex={onRowClick ? 0 : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              onKeyDown={onRowClick ? (e) => e.key === 'Enter' && onRowClick(row) : undefined}
              className={clsx('border-b border-line/60 last:border-0', onRowClick && 'cursor-pointer hover:bg-raised')}
            >
              {columns.map((c) => (
                <td key={c.key} className={clsx('px-4 py-3 align-middle', c.align === 'right' && 'text-right', c.className)}>
                  {c.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
