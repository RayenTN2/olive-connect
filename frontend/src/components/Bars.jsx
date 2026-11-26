import { Link } from 'react-router'
import './Bars.css'

// rows: { key, label, value (a number), text (how it reads), color, max?, note?, to? }. Each bar is measured against its own
// max, or against the largest value. The figures are always written, so the bar itself is decoration for screen readers.
export default function Bars({ rows }) {
  const largest = Math.max(...rows.map(({ value }) => value), 1)
  return (
    <ul className="bars">
      {rows.map(({ key, label, value, text, color, max = largest, note, to }) => {
        const share = Math.min(100, (value / (max || 1)) * 100)
        const content = (
          <>
            <span className="bar-label">{label}</span>
            <span className="bar-text">{text}</span>
            <span className="bar-track" aria-hidden="true">{value > 0 && <span style={{ width: `max(${share}%, 3px)`, background: color }} />}</span>
            {note && <span className="bar-note">{note}</span>}
          </>
        )
        return <li key={key}>{to ? <Link className="bar" to={to}>{content}</Link> : <div className="bar">{content}</div>}</li>
      })}
    </ul>
  )
}
