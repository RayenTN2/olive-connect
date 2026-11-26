import { markSvg } from '../../planning/mark.js'

export default function OrchardMark({ state, color, urgent = false, size = 20 }) {
  return <span className="mark" aria-hidden="true" dangerouslySetInnerHTML={{ __html: markSvg({ state, color, urgent, size }) }} />
}
