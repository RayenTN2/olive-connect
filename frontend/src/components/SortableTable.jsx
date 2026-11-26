// columns: [sort key or null, label]. numeric: keys aligned right
export default function SortableTable({ columns, numeric, filters, onSort, children }) {
  return (
    <table className="tours">
      <thead>
        <tr>
          {columns.map(([key, label]) => (
            <th key={label} scope="col" className={numeric.includes(key) ? 'numeric' : undefined} aria-sort={key && filters.tri === key ? (filters.sens === 'asc' ? 'ascending' : 'descending') : undefined}>
              {key ? (
                <button type="button" onClick={() => onSort(key)}>
                  {label}
                  <svg viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    {filters.tri === key && <path d={filters.sens === 'asc' ? 'm2.5 7.5 3.5-3.5 3.5 3.5' : 'm2.5 4.5 3.5 3.5 3.5-3.5'} />}
                  </svg>
                </button>
              ) : label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>{children}</tbody>
    </table>
  )
}
