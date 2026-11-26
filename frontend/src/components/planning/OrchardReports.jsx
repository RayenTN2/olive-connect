import { useEffect, useState } from 'react'
import { getMaturityHistory, getProblems } from '../../api/planning.js'
import { loadErrorMessage } from '../../auth/messages.js'
import { formatInstant, isOpenProblem, maturityOf, PROBLEMS } from '../../planning/labels.js'

function Report({ title, date, text }) {
  return (
    <li>
      <span className="report-head">{title}<time>{formatInstant(date)}</time></span>
      {text && <span className="muted">{text}</span>}
    </li>
  )
}

// Open problems and the latest maturity reports. Mounted with a key, so its state always belongs to one orchard
export default function OrchardReports({ orchardId }) {
  const [reports, setReports] = useState(null) // { maturity, problems }
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let stale = false
    Promise.all([getMaturityHistory(orchardId), getProblems(orchardId)])
      .then(([maturity, problems]) => !stale && setReports({ maturity, problems: problems.filter(isOpenProblem) }))
      .catch((failure) => !stale && setError(loadErrorMessage(failure.status)))
    return () => { stale = true }
  }, [orchardId, attempt])

  function retry() {
    setError('')
    setAttempt(attempt + 1)
  }

  return (
    <>
      <h2>Signalements</h2>
      {error && <div className="notice" role="alert"><p>{error}</p><button className="button-quiet" type="button" onClick={retry}>Réessayer</button></div>}
      {!error && !reports && <div className="skeleton" role="status"><span className="visually-hidden">Chargement des signalements</span></div>}
      {reports && reports.problems.length + reports.maturity.length === 0 && <p className="muted">Aucun signalement pour ce verger.</p>}
      {reports && (
        <ul className="reports">
          {reports.problems.map((report) => (
            <Report key={report.id} title={<strong className="urgent">{PROBLEMS[report.problemType] ?? PROBLEMS.OTHER}</strong>} date={report.reportedAt} text={report.description} />
          ))}
          {reports.maturity.slice(0, 5).map((report) => (
            <Report key={report.id} title={`Maturité : ${maturityOf(report.maturityLevel).label.toLowerCase()}`} date={report.reportedAt} text={report.notes} />
          ))}
        </ul>
      )}
    </>
  )
}
