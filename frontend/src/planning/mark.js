// One drawing for the map, list and legend. Colour = maturity, shape = planning state, so state never relies on colour.
// All inputs are constants from labels.js, never user text.
const SHAPES = {
  eligible: (color) => `<circle cx="12" cy="12" r="8" fill="${color}"/>`,
  pending: (color) => `<circle cx="12" cy="12" r="6.5" fill="#fff" stroke="${color}" stroke-width="3.5"/>`,
  scheduled: (color) => `<rect x="5" y="5" width="14" height="14" rx="2" fill="${color}"/>`,
  place: (color) => `<circle cx="12" cy="12" r="8" fill="${color}"/>`, // an orchard shown without its planning state
  none: (color) => `<circle cx="12" cy="12" r="4" fill="${color}"/>`,
}

// order: position in the tour being prepared (0 = not a stop)
export function markSvg({ state, color, urgent = false, order = 0, size }) {
  if (order > 0) {
    return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" overflow="visible"><circle cx="12" cy="12" r="13.5" fill="#fff"/><circle cx="12" cy="12" r="11.5" fill="#3f6b4f" stroke="${color}" stroke-width="2.5"/><text x="12" y="16" fill="#fff" font-size="11.5" font-weight="600" text-anchor="middle">${order}</text></svg>`
  }
  const halo = state === 'scheduled'
    ? '<rect x="3" y="3" width="18" height="18" rx="4" fill="#fff"/>'
    : `<circle cx="12" cy="12" r="${state === 'none' ? 6 : 10}" fill="#fff"/>`
  const flag = urgent
    ? '<circle cx="19" cy="5" r="5" fill="#b3261e" stroke="#fff" stroke-width="1.5"/><path d="M19 2.6v2.8M19 7.3v.1" stroke="#fff" stroke-width="1.4" stroke-linecap="round"/>'
    : ''
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" overflow="visible">${halo}${SHAPES[state](color)}${flag}</svg>`
}
