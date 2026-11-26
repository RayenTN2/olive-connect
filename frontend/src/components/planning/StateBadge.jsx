import { STATES } from '../../planning/labels.js'
import OrchardMark from './OrchardMark.jsx'

export default function StateBadge({ state }) {
  return (
    <span className={`state-badge state-${state}`}>
      <OrchardMark state={state} color="currentColor" size={14} />
      {STATES[state]}
    </span>
  )
}
