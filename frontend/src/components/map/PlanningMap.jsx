import { useEffect, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { MapContainer, Marker, Polyline, TileLayer, Tooltip, useMap, ZoomControl } from 'react-leaflet'
import { maturityOf, MATURITIES, STATES } from '../../planning/labels.js'
import { markSvg } from '../../planning/mark.js'
import OrchardMark from '../planning/OrchardMark.jsx'
import './PlanningMap.css'

const SFAX = [34.74, 10.76]
const SFAX_ZOOM = 10
const FOCUS_ZOOM = 14

const animate = () => !window.matchMedia('(prefers-reduced-motion: reduce)').matches

// What the menu and floating panels hide, so a place is centred in what remains visible.
// A map in a card is hidden by nothing: only the margins remain
function clear(map) {
  const box = map.getContainer().getBoundingClientRect()
  const page = map.getContainer().closest('main')
  const left = page?.querySelector('.float-left')?.getBoundingClientRect().right ?? document.querySelector('.menu')?.getBoundingClientRect().right ?? box.left
  const right = page?.querySelector('.float-right')?.getBoundingClientRect().left ?? box.right
  return { paddingTopLeft: [Math.max(0, left - box.left) + 50, 80], paddingBottomRight: [Math.max(0, box.right - right) + 50, 60] }
}

// Moves the map only when the choice came from the list: after a click on the map the marker is already in view
function Focus({ target }) {
  const map = useMap()
  useEffect(() => {
    if (target?.position) map.fitBounds([target.position, target.position], { ...clear(map), maxZoom: Math.max(map.getZoom(), FOCUS_ZOOM), animate: animate() })
  }, [map, target])
  return null
}

// Shows the whole saved tour once
function FitRoute({ points }) {
  const map = useMap()
  useEffect(() => {
    if (points.length > 0) map.fitBounds(points, { ...clear(map), maxZoom: FOCUS_ZOOM, animate: animate() })
  }, [map, points])
  return null
}

// Same object for the same look: Leaflet rebuilds the marker element whenever its icon object changes
const icons = new Map()
function iconFor(state, color, urgent, order) {
  const key = `${state} ${color} ${urgent} ${order}`
  if (!icons.has(key)) {
    icons.set(key, L.divIcon({ className: 'orchard-marker', html: markSvg({ state, color, urgent, order, size: 28 }), iconSize: [32, 32] }))
  }
  return icons.get(key)
}

const millSvg = (inTour, size) => `<svg viewBox="0 0 24 24" width="${size}" height="${size}" overflow="visible"><rect x="1" y="1" width="22" height="22" rx="7.5" fill="${inTour ? '#2f6fb5' : '#fff'}" stroke="${inTour ? '#fff' : '#2f6fb5'}" stroke-width="2"/><path d="M6.5 17v-6l3.2 2.2V11l3.2 2.2V8h4.6v9Z" fill="none" stroke="${inTour ? '#fff' : '#2f6fb5'}" stroke-width="1.6" stroke-linejoin="round"/></svg>`
const millIcon = (inTour) => L.divIcon({ className: 'mill-marker', html: millSvg(inTour, inTour ? 32 : 28), iconSize: [32, 32] })
const MILL_ICONS = { true: millIcon(true), false: millIcon(false) }

// Leaflet measures its container once; the map changes width when the tour panel opens
function FollowSize() {
  const map = useMap()
  useEffect(() => {
    const observer = new ResizeObserver(() => map.invalidateSize({ pan: false }))
    observer.observe(map.getContainer())
    return () => observer.disconnect()
  }, [map])
  return null
}

function OrchardMarker({ row, order, highlighted, selected, onSelect, onHover }) {
  const [marker, setMarker] = useState(null)
  const urgent = row.harvest?.requestType === 'URGENT'
  const { state } = row
  const { color, label } = maturityOf(row.maturity)
  const icon = iconFor(state, color, urgent, order)
  const situation = order > 0 ? `Arrêt ${order} de la tournée` : STATES[state] ?? 'Verger'
  // A saved tour says nothing about its orchards' maturity
  const maturity = row.maturity === undefined ? '' : `, maturité ${label.toLowerCase()}`
  const description = `${row.name}, ${situation}${maturity}${urgent ? ', urgente' : ''}`

  // Classes are toggled on the element: a new icon would replace it and lose keyboard focus
  useEffect(() => {
    const element = marker?.getElement()
    if (!element) return
    element.setAttribute('aria-label', description)
    element.classList.toggle('selected', selected)
    element.classList.toggle('highlighted', highlighted)
  }, [marker, icon, description, selected, highlighted])

  return (
    <Marker
      ref={setMarker} position={row.position} icon={icon} zIndexOffset={selected ? 3000 : highlighted ? 2000 : order > 0 ? 1000 : 0}
      eventHandlers={{
        click: () => onSelect(row.id),
        // Leaflet focuses markers but doesn't turn Enter or Space into a click
        keydown: ({ originalEvent }) => {
          if (originalEvent.key !== 'Enter' && originalEvent.key !== ' ') return
          originalEvent.preventDefault()
          onSelect(row.id)
        },
        mouseover: () => onHover(row.id),
        mouseout: () => onHover(null),
      }}
    >
      <Tooltip direction="top" offset={[0, -14]}><strong>{row.name}</strong><br />{situation}</Tooltip>
    </Marker>
  )
}

function MillMarker({ mill, inTour, selected, highlighted, refocus, onSelect }) {
  const [marker, setMarker] = useState(null)
  const icon = MILL_ICONS[inTour]
  const description = `${mill.name}, ${inTour ? 'pressoir de la tournée' : 'pressoir'}`
  useEffect(() => {
    const element = marker?.getElement()
    if (!element) return
    element.setAttribute('aria-label', description)
    element.classList.toggle('selected', selected)
    element.classList.toggle('highlighted', highlighted)
  }, [marker, icon, description, selected, highlighted])
  // Pointed at in the tour panel: name it on the map, and bring it into view if it's hidden.
  // A short pause, so passing over the list on the way elsewhere doesn't move the map
  const map = useMap()
  useEffect(() => {
    if (!marker || !highlighted) return undefined
    marker.openTooltip()
    const timer = setTimeout(() => map.panInside(marker.getLatLng(), { ...clear(map), animate: animate() }), 150)
    return () => {
      clearTimeout(timer)
      marker.closeTooltip()
    }
  }, [map, marker, highlighted])
  // Back from its details: put the keyboard where it was
  useEffect(() => { if (refocus) marker?.getElement()?.focus({ preventScroll: true }) }, [marker, refocus])

  return (
    <Marker
      ref={setMarker} position={[mill.latitude, mill.longitude]} icon={icon} zIndexOffset={selected ? 3000 : highlighted ? 2000 : -500}
      eventHandlers={{
        click: () => onSelect(mill.id),
        keydown: ({ originalEvent }) => {
          if (originalEvent.key !== 'Enter' && originalEvent.key !== ' ') return
          originalEvent.preventDefault()
          onSelect(mill.id)
        },
      }}
    >
      <Tooltip direction="top" offset={[0, -14]}><strong>{mill.name}</strong><br />{inTour ? 'Pressoir de la tournée' : 'Pressoir'}</Tooltip>
    </Marker>
  )
}

function MillMark({ inTour }) {
  return <span className="mark" aria-hidden="true" dangerouslySetInnerHTML={{ __html: millSvg(inTour, 16) }} />
}

function Legend({ single, withStops, withMills, withTourMills, withRoute, realRoute }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="legend">
      <button type="button" className="legend-toggle" aria-expanded={open} onClick={() => setOpen(!open)}>Légende</button>
      {open && (
        <div className="legend-body">
          <ul>
            {single && withStops && <li><OrchardMark state="eligible" color="var(--olive)" size={16} />Arrêt de la tournée</li>}
            {!single && Object.entries(STATES).map(([state, label]) => <li key={state}><OrchardMark state={state} color="var(--soft)" size={16} />{label}</li>)}
            {!single && <li><OrchardMark state="eligible" color="var(--soft)" urgent size={16} />Demande urgente</li>}
            {withMills && <li><MillMark inTour={false} />Pressoir</li>}
            {withTourMills && <li><MillMark inTour />Pressoir de la tournée</li>}
            {withRoute && <li><span className="mark" aria-hidden="true"><svg viewBox="0 0 16 16" width="16" height="16"><path d="M1 8h14" stroke="var(--blue)" strokeWidth="3" /></svg></span>{realRoute ? 'Itinéraire routier' : 'Trajet estimé'}</li>}
          </ul>
          {!single && <ul>
            {Object.entries(MATURITIES).map(([key, { label, color }]) => <li key={key}><i style={{ background: color }} />{label}</li>)}
          </ul>}
        </div>
      )}
    </div>
  )
}

export default function PlanningMap({ single = false, rows, stopIds, mills, tourMillIds, millsError, onRetryMills, route, selectedId, selectedMillId, returnToMillId, hoveredId, focusTarget, onSelect, onSelectMill, onHover }) {
  const [map, setMap] = useState(null)
  const [tilesFailed, setTilesFailed] = useState(false)
  const placed = rows.filter((row) => row.position)
  const placedMills = mills.filter(({ latitude, longitude }) => latitude != null && longitude != null)
  const everything = [...placed.map((row) => row.position), ...placedMills.map(({ latitude, longitude }) => [latitude, longitude])]
  const view = { animate: animate() }

  return (
    <section className="map" aria-label="Carte des vergers">
      <MapContainer ref={setMap} center={SFAX} zoom={SFAX_ZOOM} zoomControl={false} className="map-canvas">
        <TileLayer
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" maxZoom={19} className="map-tiles"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          eventHandlers={{ tileerror: () => setTilesFailed(true), tileload: () => setTilesFailed(false) }}
        />
        <ZoomControl position="topright" zoomInTitle="Zoom avant" zoomOutTitle="Zoom arrière" />
        <FollowSize />
        <Focus target={focusTarget} />
        {route && <FitRoute points={route.points} />}
        {route?.line.length > 1 && (
          <>
            <Polyline positions={route.line} interactive={false} pathOptions={{ color: '#fff', weight: 9, opacity: 0.95 }} smoothFactor={0.3} />
            <Polyline positions={route.line} interactive={false} pathOptions={{ color: '#2f6fb5', weight: 5 }} smoothFactor={0.3} />
          </>
        )}
        {placed.map((row) => (
          <OrchardMarker
            key={row.id} row={row} order={stopIds.indexOf(row.id) + 1} selected={row.id === selectedId} highlighted={row.id === hoveredId}
            onSelect={onSelect} onHover={onHover}
          />
        ))}
        {placedMills.map((mill) => (
          <MillMarker
            key={mill.id} mill={mill} inTour={tourMillIds.includes(mill.id)} selected={mill.id === selectedMillId} highlighted={mill.id === hoveredId}
            refocus={mill.id === returnToMillId} onSelect={onSelectMill}
          />
        ))}
      </MapContainer>
      <div className="map-views">
        <button type="button" onClick={() => map.fitBounds([SFAX, SFAX], { ...clear(map), maxZoom: SFAX_ZOOM, ...view })}>Sfax</button>
        <button
          type="button" disabled={everything.length === 0}
          onClick={() => map.fitBounds(everything, { ...clear(map), maxZoom: FOCUS_ZOOM, ...view })}
        >
          Tout afficher
        </button>
      </div>
      <div className="map-notices">
        {tilesFailed && <p className="map-notice" role="status">Le fond de carte ne se charge pas. Les vergers restent à leur place.</p>}
        {millsError && <div className="map-notice" role="alert"><p>Les pressoirs ne se chargent pas. {millsError}</p><button className="button-quiet" type="button" onClick={onRetryMills}>Réessayer</button></div>}
      </div>
      {(!single || stopIds.length > 0) && <Legend single={single} withStops={stopIds.length > 0} withMills={placedMills.some(({ id }) => !tourMillIds.includes(id))} withTourMills={placedMills.some(({ id }) => tourMillIds.includes(id))} withRoute={route?.line.length > 1} realRoute={route?.real} />}
    </section>
  )
}
