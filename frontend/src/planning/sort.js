const compare = (a, b) => (typeof a === 'string' ? a.localeCompare(b, 'fr') : a - b)

// value: item → comparable or null. Missing values come last in both directions; tie: item → number, smallest first
export function sortWith(items, value, sens, tie) {
  const direction = sens === 'asc' ? 1 : -1
  return [...items].sort((a, b) => {
    const [x, y] = [value(a) ?? null, value(b) ?? null]
    if ((x === null) !== (y === null)) return x === null ? 1 : -1
    return (x === null ? 0 : direction * compare(x, y)) || tie(a) - tie(b)
  })
}

export const time = (instant) => (instant ? Date.parse(instant) : null)

// Sorting from the address: an unknown column or direction falls back
export function readSort(params, columns, fallback, descendingFirst) {
  const tri = columns.includes(params.get('tri')) ? params.get('tri') : fallback
  const sens = ['asc', 'desc'].includes(params.get('sens')) ? params.get('sens') : descendingFirst.includes(tri) ? 'desc' : 'asc'
  return { tri, sens }
}
