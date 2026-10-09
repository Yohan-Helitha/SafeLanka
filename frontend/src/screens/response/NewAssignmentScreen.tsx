import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Building2,
  ChevronDown,
  Compass,
  Crosshair,
  FileText,
  Loader2,
  MapPin,
  Search,
  Send,
  Truck,
  Users,
  X,
} from 'lucide-react'
import { ApiErrorNotice } from '@/components/domain'
import { MapboxMap, type MapMarker } from '@/components/ui'
import { TEAM_TYPE_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { isApiError } from '@/services'
import { useDocumentTitle, useOnlineStatus, useReferenceData } from '@/hooks/shared'
import { useCreateAssignment, useShelters, useShelterSuggestions, useTeams } from '@/hooks/response/useResponse'
import { compact } from '@/utils/validation'

interface Alt {
  id: string
  name: string
}

interface GeocodeFeature {
  id: string
  place_name: string
  text: string
  center: [number, number] // [lng, lat]
}

export function NewAssignmentScreen() {
  useDocumentTitle('New assignment')
  const online = useOnlineStatus()
  const user = useCurrentUser()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { events, districtName } = useReferenceData()
  const teams = useTeams(user.districtId)
  const allShelters = useShelters({ districtId: user.districtId })
  const create = useCreateAssignment()

  const [attempted, setAttempted] = useState(false)
  const [locationText, setLocationText] = useState('')
  const [lat, setLat] = useState('')
  const [lng, setLng] = useState('')
  const [mapCenter, setMapCenter] = useState<[number, number]>([79.8612, 6.9271])
  const [task, setTask] = useState('')
  const [priority, setPriority] = useState<1 | 2 | 3>(2)
  const [people, setPeople] = useState('')
  const [shelterId, setShelterId] = useState('')
  const [teamId, setTeamId] = useState('')

  // Geocoding auto-search state
  const [isSearching, setIsSearching] = useState(false)
  const [searchResults, setSearchResults] = useState<GeocodeFeature[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const skipSearchRef = useRef(false)
  const searchContainerRef = useRef<HTMLDivElement>(null)

  const latN = Number(lat)
  const lngN = Number(lng)
  const coordsOk = lat !== '' && lng !== '' && Number.isFinite(latN) && Number.isFinite(lngN)
  const suggestions = useShelterSuggestions(coordsOk ? latN : null, coordsOk ? lngN : null)
  const event = events.find((e) => e.status === 'ACTIVE' && e.districtIds.includes(user.districtId)) ?? events.find((e) => e.status === 'ACTIVE')

  const districtLabel = districtName(user.districtId)
  const sectorName = districtLabel && districtLabel !== '—' ? `${districtLabel.toUpperCase()} DISTRICT` : 'WESTERN REGION'

  // Available open shelters with remaining space
  const availableShelters = useMemo(() => {
    const suggestionsList = suggestions.data ?? []
    const districtList = allShelters.data ?? []
    const combinedMap = new Map<string, {
      id: string
      name: string
      capacity: number
      freeCapacity: number
      distanceKm?: number
      status: string
      latitude: number
      longitude: number
    }>()

    // Add district shelters that are open with free capacity
    districtList.forEach((s) => {
      const free = Math.max(0, s.capacity - (s.currentOccupancy ?? 0))
      if (s.status === 'OPEN' && free > 0) {
        combinedMap.set(s.id, {
          id: s.id,
          name: s.name,
          capacity: s.capacity,
          freeCapacity: free,
          distanceKm: undefined,
          status: s.status,
          latitude: s.latitude,
          longitude: s.longitude,
        })
      }
    })

    // Overlay proximity suggestions which include calculated distanceKm
    suggestionsList.forEach((s) => {
      if (s.freeCapacity > 0) {
        combinedMap.set(s.id, {
          id: s.id,
          name: s.name,
          capacity: s.capacity,
          freeCapacity: s.freeCapacity,
          distanceKm: s.distanceKm,
          status: s.status,
          latitude: s.latitude,
          longitude: s.longitude,
        })
      }
    })

    return Array.from(combinedMap.values()).sort((a, b) => {
      if (a.distanceKm != null && b.distanceKm != null) return a.distanceKm - b.distanceKm
      if (a.distanceKm != null) return -1
      if (b.distanceKm != null) return 1
      return b.freeCapacity - a.freeCapacity
    })
  }, [suggestions.data, allShelters.data])

  // Map markers: Assignment Target location only (existing shelter locations hidden)
  const mapMarkers = useMemo<MapMarker[]>(() => {
    if (!coordsOk) return []
    return [
      {
        id: 'here',
        latitude: latN,
        longitude: lngN,
        label: locationText.trim() ? `Target: ${locationText.trim()}` : 'Assignment target',
        tone: 'danger',
      },
    ]
  }, [coordsOk, latN, lngN, locationText])

  const mapboxToken = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN

  // Close search suggestions on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(e.target as Node)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Robust geocoding search covering Mapbox with fallbacks
  const executeGeocode = useCallback(
    async (rawQuery: string) => {
      const query = rawQuery.trim()
      if (query.length < 2) {
        setSearchResults([])
        setIsSearching(false)
        return
      }

      setIsSearching(true)
      try {
        let features: GeocodeFeature[] = []

        // 1. Primary: Mapbox Places API with country=lk and proximity to Colombo / Sri Lanka
        if (mapboxToken) {
          try {
            const res = await fetch(
              `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${mapboxToken}&country=lk&proximity=79.8612,6.9271&limit=5`,
            )
            if (res.ok) {
              const data = await res.json()
              if (data.features && data.features.length > 0) {
                features = data.features.map((f: any) => ({
                  id: f.id,
                  place_name: f.place_name,
                  text: f.text,
                  center: f.center,
                }))
              }
            }
          } catch {
            // Mapbox network failover
          }

          // 2. Mapbox Fallback: Append "Sri Lanka" if initial query returned no results
          if (features.length === 0 && !query.toLowerCase().includes('sri lanka')) {
            try {
              const resFallback = await fetch(
                `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query + ', Sri Lanka')}.json?access_token=${mapboxToken}&limit=5`,
              )
              if (resFallback.ok) {
                const dataFallback = await resFallback.json()
                if (dataFallback.features && dataFallback.features.length > 0) {
                  features = dataFallback.features.map((f: any) => ({
                    id: f.id,
                    place_name: f.place_name,
                    text: f.text,
                    center: f.center,
                  }))
                }
              }
            } catch {
              // Ignore and proceed to Nominatim
            }
          }
        }

        // 3. Fallback: OpenStreetMap Nominatim for Sri Lanka
        if (features.length === 0) {
          try {
            const nomQuery = query.toLowerCase().includes('sri lanka') ? query : `${query}, Sri Lanka`
            const nomRes = await fetch(
              `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(nomQuery)}&countrycodes=lk&limit=5`,
            )
            if (nomRes.ok) {
              const nomData = await nomRes.json()
              if (Array.isArray(nomData) && nomData.length > 0) {
                features = nomData.map((item: any) => ({
                  id: String(item.place_id),
                  text: item.name || item.display_name.split(',')[0],
                  place_name: item.display_name,
                  center: [parseFloat(item.lon), parseFloat(item.lat)],
                }))
              }
            }
          } catch {
            // Ignore
          }
        }

        setSearchResults(features)

        if (features.length > 0) {
          const top = features[0]
          const [topLng, topLat] = top.center
          setLat(topLat.toFixed(5))
          setLng(topLng.toFixed(5))
          setMapCenter([topLng, topLat])
          setShowSuggestions(true)
        }
      } finally {
        setIsSearching(false)
      }
    },
    [mapboxToken],
  )

  // Auto-search location on typing with 350ms debounce
  useEffect(() => {
    if (skipSearchRef.current) {
      skipSearchRef.current = false
      return
    }

    const query = locationText.trim()
    if (query.length < 2) {
      setSearchResults([])
      setIsSearching(false)
      return
    }

    const timer = setTimeout(() => {
      executeGeocode(locationText)
    }, 350)

    return () => clearTimeout(timer)
  }, [locationText, executeGeocode])

  // Manual location selection via map click
  const handleMapPick = async (pickedLat: number, pickedLng: number) => {
    setLat(pickedLat.toFixed(5))
    setLng(pickedLng.toFixed(5))
    setMapCenter([pickedLng, pickedLat])
    setShowSuggestions(false)
    setSearchResults([])

    if (!mapboxToken) return
    try {
      const res = await fetch(
        `https://api.mapbox.com/geocoding/v5/mapbox.places/${pickedLng},${pickedLat}.json?access_token=${mapboxToken}&country=lk&limit=1`,
      )
      if (res.ok) {
        const data = await res.json()
        if (data.features && data.features.length > 0) {
          skipSearchRef.current = true
          setLocationText(data.features[0].place_name.replace(', Sri Lanka', ''))
          return
        }
      }
    } catch {
      // Ignore reverse-geocode failures
    }

    // Fallback: Nominatim reverse geocode
    try {
      const nomRes = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${pickedLat}&lon=${pickedLng}`,
      )
      if (nomRes.ok) {
        const nomData = await nomRes.json()
        if (nomData.display_name) {
          skipSearchRef.current = true
          setLocationText(nomData.display_name.replace(', Sri Lanka', ''))
        }
      }
    } catch {
      // Ignore
    }
  }

  // Manual latitude / longitude field inputs
  const handleManualLatChange = (value: string) => {
    setLat(value)
    const n = Number(value)
    if (value !== '' && Number.isFinite(n) && Number.isFinite(lngN)) {
      setMapCenter([lngN, n])
    }
  }

  const handleManualLngChange = (value: string) => {
    setLng(value)
    const n = Number(value)
    if (value !== '' && Number.isFinite(n) && Number.isFinite(latN)) {
      setMapCenter([n, latN])
    }
  }

  // Select place from geocoding suggestions dropdown
  const handleSelectSuggestion = (feature: GeocodeFeature) => {
    skipSearchRef.current = true
    setLocationText(feature.place_name.replace(', Sri Lanka', ''))
    const [sLng, sLat] = feature.center
    setLat(sLat.toFixed(5))
    setLng(sLng.toFixed(5))
    setMapCenter([sLng, sLat])
    setShowSuggestions(false)
    setSearchResults([])
  }

  const errors = compact({
    locationText: locationText.trim().length >= 3 ? null : 'Add a valid place name (at least 3 characters).',
    location: coordsOk ? null : 'Enter valid numeric latitude and longitude.',
    task: task.trim().length >= 5 && task.trim().length <= 500 ? null : 'Describe the task in 5–500 characters.',
    people: people === '' || (Number.isInteger(Number(people)) && Number(people) >= 0) ? null : 'Enter a number of people, 0 or more.',
    event: event ? null : 'There is no active event to attach this assignment to.',
  })

  const show = (k: string) => (attempted ? (errors[k] ?? null) : null)
  const alternatives =
    isApiError(create.error) && create.error.code === 'TEAM_NOT_AVAILABLE' ? ((create.error.details?.alternatives as Alt[] | undefined) ?? []) : []
  const availableTeams = (teams.data ?? []).filter((t) => t.status === 'AVAILABLE')

  const submit = () => {
    setAttempted(true)
    if (!online || Object.keys(errors).length || !event) return
    create.mutate(
      {
        eventId: event.id,
        teamId: teamId || null,
        districtId: user.districtId,
        latitude: latN,
        longitude: lngN,
        locationText: locationText.trim(),
        task: task.trim(),
        priority,
        peopleEstimated: people === '' ? 0 : Number(people),
        destinationShelterId: shelterId || null,
      },
      {
        onSuccess: () => {
          toast(teamId ? 'Team dispatched successfully' : 'Assignment registered successfully')
          navigate(paths.district.teams)
        },
      },
    )
  }

  return (
    <form
      noValidate
      onSubmit={(e) => {
        e.preventDefault()
        submit()
      }}
      className="mx-auto w-full max-w-[1280px] space-y-6"
    >
      {/* Breadcrumb & Tactical Page Header */}
      <div className="flex flex-col gap-2 pt-1">
        <Link
          to={paths.district.teams}
          className="group inline-flex w-fit items-center gap-1.5 text-xs font-semibold text-slate-400 transition-colors hover:text-cyan-400"
        >
          <ArrowLeft className="h-4 w-4 transition-transform group-hover:-translate-x-0.5" />
          <span>Rescue teams</span>
        </Link>
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold tracking-tight text-white sm:text-3xl">New assignment</h1>
            <p className="mt-1 text-sm text-slate-400">Describe the task, then dispatch a team now or assign one later.</p>
          </div>
          <div className="flex items-center gap-2 self-start rounded-lg border border-[#1e2a42] bg-[#101726] px-3.5 py-1.5 text-xs text-slate-300 shadow-sm md:self-auto">
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-cyan-400 opacity-75" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-cyan-400" />
            </span>
            <span className="font-mono font-medium tracking-wide text-cyan-300">DISPATCH SECTOR: {sectorName}</span>
          </div>
        </div>
      </div>

      {/* 2-Column Responsive Tactical Grid */}
      <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-12">
        {/* LEFT COLUMN: Where / GPS Telemetry (6 cols) */}
        <div className="flex h-full flex-col gap-6 lg:col-span-6">
          <section className="flex h-full flex-1 flex-col gap-4 rounded-2xl border border-[#253046] bg-[#161e2e]/95 p-5 shadow-lg">
            <div className="flex items-center justify-between border-b border-[#253046]/80 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 shadow-sm">
                  <Compass className="h-4 w-4" />
                </div>
                <h2 className="text-base font-bold tracking-tight text-white">Where</h2>
              </div>
              <span className="rounded border border-[#2e3b52] bg-[#101726] px-2.5 py-0.5 text-[11px] font-medium text-slate-300">
                GPS Telemetry
              </span>
            </div>

            {/* Place Field with Auto-Search & Geocoding */}
            <div ref={searchContainerRef} className="relative space-y-1.5">
              <div className="flex items-center justify-between">
                <label htmlFor="place-input" className="flex items-center gap-1 text-xs font-semibold uppercase tracking-wider text-slate-300">
                  <span>Place</span>
                  <span className="font-bold text-rose-400">*</span>
                </label>
                {isSearching && (
                  <span className="flex items-center gap-1 text-[11px] font-mono text-cyan-400">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    Locating on map...
                  </span>
                )}
              </div>
              <div className="relative flex items-center">
                <MapPin className="pointer-events-none absolute left-3.5 h-4 w-4 text-slate-500" />
                <input
                  id="place-input"
                  type="text"
                  value={locationText}
                  onChange={(e) => {
                    setLocationText(e.target.value)
                    setShowSuggestions(true)
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      executeGeocode(locationText)
                    }
                  }}
                  onFocus={() => {
                    if (searchResults.length > 0) setShowSuggestions(true)
                  }}
                  placeholder="Station Road, Kolonnawa"
                  className={`w-full rounded-lg border bg-[#0c121e] py-2.5 pl-10 pr-24 text-sm text-slate-100 placeholder:text-slate-500 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 ${
                    show('locationText') ? 'border-rose-500 focus:border-rose-500 focus:ring-rose-500' : 'border-[#232c3f]'
                  }`}
                />
                <div className="absolute right-2 flex items-center gap-1">
                  {locationText && (
                    <button
                      type="button"
                      onClick={() => {
                        setLocationText('')
                        setSearchResults([])
                        setShowSuggestions(false)
                      }}
                      className="p-1 text-slate-500 transition-colors hover:text-slate-300"
                      title="Clear location"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => executeGeocode(locationText)}
                    disabled={isSearching || !locationText.trim()}
                    className="flex h-7 items-center gap-1 rounded border border-cyan-500/30 bg-cyan-950/60 px-2 text-[11px] font-medium text-cyan-300 transition-colors hover:bg-cyan-900/60 disabled:opacity-40"
                    title="Search location on map"
                  >
                    {isSearching ? <Loader2 className="h-3 w-3 animate-spin" /> : <Search className="h-3 w-3" />}
                    <span>Locate</span>
                  </button>
                </div>
              </div>

              {/* Geocoding Auto-Search Suggestions Dropdown */}
              {showSuggestions && searchResults.length > 0 && (
                <div className="absolute left-0 right-0 top-full z-30 mt-1 max-h-56 overflow-y-auto rounded-lg border border-[#2e3b52] bg-[#0c121e] py-1.5 shadow-2xl backdrop-blur-md">
                  <div className="px-3 py-1 text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                    Matching Locations (Click to pick)
                  </div>
                  {searchResults.map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => handleSelectSuggestion(f)}
                      className="flex w-full items-start gap-2.5 px-3 py-2 text-left text-xs text-slate-200 transition-colors hover:bg-cyan-950/40 hover:text-cyan-300"
                    >
                      <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0 text-cyan-400" />
                      <div className="min-w-0 flex-1">
                        <div className="font-semibold text-white">{f.text}</div>
                        <div className="truncate text-[11px] text-slate-400">{f.place_name}</div>
                      </div>
                    </button>
                  ))}
                </div>
              )}

              {show('locationText') && <p className="text-xs text-rose-400">{show('locationText')}</p>}
            </div>

            {/* Interactive Mapbox Tactical Canvas */}
            <div className="flex flex-1 flex-col gap-2 min-h-[460px]">
              <div className="group relative h-full min-h-[440px] w-full flex-1 overflow-hidden rounded-xl border border-[#232c3f] bg-[#0c121e] shadow-inner select-none">
                <MapboxMap
                  height="100%"
                  label="Mapbox tactical assignment map"
                  darkTheme={false}
                  center={mapCenter}
                  zoom={14}
                  markers={mapMarkers}
                  selectedId={coordsOk ? 'here' : undefined}
                  onPick={handleMapPick}
                />

                {/* Tactical Reticle Live Coordinate Readout */}
                <div className="pointer-events-none absolute right-3 top-3 z-10 flex items-center gap-1.5 rounded-md border border-[#232c3f] bg-[#0c121e]/90 px-2.5 py-1 text-[11px] font-mono text-cyan-400 shadow-md backdrop-blur-md">
                  <Crosshair className="h-3.5 w-3.5 text-cyan-400" />
                  <span>{coordsOk ? `${latN.toFixed(4)}° N, ${lngN.toFixed(4)}° E` : 'No coordinates'}</span>
                </div>
              </div>

              {/* Map Instructions & Legend Caption */}
              <div className="flex items-center justify-between px-1 text-xs text-slate-400">
                <div className="flex items-center gap-2">
                  <span className="flex h-2 w-2 shrink-0 rounded-full bg-rose-500 animate-pulse" />
                  <span>Click anywhere on the map to set or update the assignment location pin.</span>
                </div>
                <span className="hidden font-mono text-[11px] text-cyan-400 sm:inline-block">Mapbox Streets Day Mode</span>
              </div>
            </div>

            {/* Manual Coordinate Dual Input Row */}
            <div className="grid grid-cols-2 gap-4 pt-1">
              <div className="space-y-1.5">
                <label htmlFor="lat-input" className="flex items-center gap-1 text-xs font-semibold uppercase tracking-wider text-slate-300">
                  <span>Latitude</span>
                  <span className="font-bold text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    id="lat-input"
                    type="text"
                    inputMode="decimal"
                    value={lat}
                    onChange={(e) => handleManualLatChange(e.target.value)}
                    placeholder="6.9335"
                    className={`w-full rounded-lg border bg-[#0c121e] px-3.5 py-2.5 font-mono text-sm text-slate-100 placeholder:text-slate-500 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 ${
                      show('location') ? 'border-rose-500' : 'border-[#232c3f]'
                    }`}
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <label htmlFor="lng-input" className="flex items-center gap-1 text-xs font-semibold uppercase tracking-wider text-slate-300">
                  <span>Longitude</span>
                  <span className="font-bold text-rose-400">*</span>
                </label>
                <div className="relative">
                  <input
                    id="lng-input"
                    type="text"
                    inputMode="decimal"
                    value={lng}
                    onChange={(e) => handleManualLngChange(e.target.value)}
                    placeholder="79.8885"
                    className={`w-full rounded-lg border bg-[#0c121e] px-3.5 py-2.5 font-mono text-sm text-slate-100 placeholder:text-slate-500 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 ${
                      show('location') ? 'border-rose-500' : 'border-[#232c3f]'
                    }`}
                  />
                </div>
              </div>
            </div>
            {show('location') && <p className="text-xs text-rose-400">{show('location')}</p>}
          </section>
        </div>

        {/* RIGHT COLUMN: Task & Team (6 cols) */}
        <div className="flex flex-col gap-6 lg:col-span-6">
          {/* Task Card */}
          <section className="flex flex-col gap-4 rounded-2xl border border-[#253046] bg-[#161e2e]/95 p-5 shadow-lg">
            <div className="flex items-center justify-between border-b border-[#253046]/80 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-amber-500/30 bg-amber-500/10 text-amber-400 shadow-sm">
                  <FileText className="h-4 w-4" />
                </div>
                <h2 className="text-base font-bold tracking-tight text-white">Task</h2>
              </div>
              <span className="rounded border border-cyan-500/30 bg-cyan-950/60 px-2.5 py-0.5 text-[11px] font-medium text-cyan-300">
                Mission Details
              </span>
            </div>

            {/* What needs to be done */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label htmlFor="task-description" className="flex items-center gap-1 text-xs font-semibold uppercase tracking-wider text-slate-300">
                  <span>What needs to be done</span>
                  <span className="font-bold text-rose-400">*</span>
                </label>
                <span className="font-mono text-[11px] text-slate-500">{task.length}/500</span>
              </div>
              <textarea
                id="task-description"
                rows={4}
                value={task}
                onChange={(e) => setTask(e.target.value)}
                placeholder="Bring 12 people out of flooded houses and take them to the shelter."
                className={`w-full resize-y rounded-lg border bg-[#0c121e] p-3 text-sm leading-relaxed text-slate-100 placeholder:text-slate-500 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 ${
                  show('task') ? 'border-rose-500 focus:border-rose-500 focus:ring-rose-500' : 'border-[#232c3f]'
                }`}
              />
              {show('task') && <p className="text-xs text-rose-400">{show('task')}</p>}
            </div>

            {/* Priority Segmented Control */}
            <div className="space-y-1.5">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">Priority</span>
              <div aria-label="Assignment Priority" className="grid grid-cols-3 gap-2 rounded-lg border border-[#232c3f] bg-[#0c121e] p-1" role="radiogroup">
                {/* Urgent Option */}
                <button
                  type="button"
                  role="radio"
                  aria-checked={priority === 1}
                  onClick={() => setPriority(1)}
                  className={`flex items-center justify-center gap-1.5 rounded-md px-3 py-2 text-xs font-semibold transition-all ${
                    priority === 1
                      ? 'border border-rose-500/60 bg-rose-950/70 text-rose-200 shadow-[0_0_12px_rgba(244,63,94,0.25)]'
                      : 'text-slate-400 hover:bg-[#162033] hover:text-slate-200'
                  }`}
                >
                  <span className={`h-2 w-2 rounded-full bg-rose-500 ${priority === 1 ? 'animate-pulse' : ''}`} />
                  <span>Urgent</span>
                </button>

                {/* High Option */}
                <button
                  type="button"
                  role="radio"
                  aria-checked={priority === 2}
                  onClick={() => setPriority(2)}
                  className={`flex items-center justify-center gap-1.5 rounded-md px-3 py-2 text-xs font-semibold transition-all ${
                    priority === 2
                      ? 'border border-cyan-500/60 bg-cyan-950/70 text-cyan-200 shadow-[0_0_12px_rgba(6,182,212,0.25)]'
                      : 'text-slate-400 hover:bg-[#162033] hover:text-slate-200'
                  }`}
                >
                  <span className={`h-2 w-2 rounded-full bg-cyan-400 ${priority === 2 ? 'animate-pulse' : ''}`} />
                  <span>High</span>
                </button>

                {/* Routine Option */}
                <button
                  type="button"
                  role="radio"
                  aria-checked={priority === 3}
                  onClick={() => setPriority(3)}
                  className={`flex items-center justify-center gap-1.5 rounded-md px-3 py-2 text-xs font-semibold transition-all ${
                    priority === 3
                      ? 'border border-emerald-500/60 bg-emerald-950/70 text-emerald-200 shadow-[0_0_12px_rgba(16,185,129,0.25)]'
                      : 'text-slate-400 hover:bg-[#162033] hover:text-slate-200'
                  }`}
                >
                  <span className="h-2 w-2 rounded-full bg-emerald-400" />
                  <span>Routine</span>
                </button>
              </div>
            </div>

            {/* People to help (estimate) */}
            <div className="space-y-1.5">
              <label htmlFor="people-estimate" className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                People to help (estimate)
              </label>
              <div className="relative flex items-center">
                <Users className="pointer-events-none absolute left-3.5 h-4 w-4 text-slate-500" />
                <input
                  id="people-estimate"
                  type="number"
                  min={0}
                  inputMode="numeric"
                  value={people}
                  onChange={(e) => setPeople(e.target.value)}
                  placeholder="12"
                  className={`w-full rounded-lg border bg-[#0c121e] py-2.5 pl-10 pr-4 font-mono text-sm text-slate-100 placeholder:text-slate-500 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 ${
                    show('people') ? 'border-rose-500' : 'border-[#232c3f]'
                  }`}
                />
              </div>
              {show('people') && <p className="text-xs text-rose-400">{show('people')}</p>}
            </div>

            {/* Take them to (Available Shelters Selector) */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label htmlFor="destination-shelter" className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                  Take them to
                </label>
                <span className="text-[11px] font-mono text-emerald-400">
                  {availableShelters.length} shelter{availableShelters.length === 1 ? '' : 's'} with space
                </span>
              </div>
              <div className="relative flex items-center">
                <Building2 className="pointer-events-none absolute left-3.5 h-4 w-4 text-slate-500" />
                <select
                  id="destination-shelter"
                  value={shelterId}
                  onChange={(e) => setShelterId(e.target.value)}
                  className="w-full cursor-pointer appearance-none rounded-lg border border-[#232c3f] bg-[#0c121e] py-2.5 pl-10 pr-10 text-sm text-slate-100 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
                >
                  <option value="">No destination yet (assign later)</option>
                  {availableShelters.length === 0 ? (
                    <option value="" disabled>
                      No open shelters with available space
                    </option>
                  ) : (
                    availableShelters.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} · {s.freeCapacity} spaces free (Cap: {s.capacity}){s.distanceKm != null ? ` · ${s.distanceKm} km away` : ''}
                      </option>
                    ))
                  )}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3.5 h-4 w-4 text-slate-400" />
              </div>
              <p className="text-xs text-slate-400">Open shelters in sector sorted by proximity and available free capacity.</p>
            </div>
          </section>

          {/* Team Dispatch Card */}
          <section className="flex flex-col gap-4 rounded-2xl border border-[#253046] bg-[#161e2e]/95 p-5 shadow-lg">
            <div className="flex items-center justify-between border-b border-[#253046]/80 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 shadow-sm">
                  <Truck className="h-4 w-4" />
                </div>
                <h2 className="text-base font-bold tracking-tight text-white">Team</h2>
              </div>
              <span className="rounded border border-[#2e3b52] bg-[#101726] px-2.5 py-0.5 text-[11px] font-medium text-slate-300">
                Roster Allocation
              </span>
            </div>

            {/* Dispatch now Selector */}
            <div className="space-y-1.5">
              <label htmlFor="dispatch-team" className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Dispatch now
              </label>
              <div className="relative flex items-center">
                <Truck className="pointer-events-none absolute left-3.5 h-4 w-4 text-slate-500" />
                <select
                  id="dispatch-team"
                  value={teamId}
                  onChange={(e) => setTeamId(e.target.value)}
                  className="w-full cursor-pointer appearance-none rounded-lg border border-[#232c3f] bg-[#0c121e] py-2.5 pl-10 pr-10 text-sm text-slate-100 outline-none transition-all focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
                >
                  <option value="">Assign later</option>
                  {availableTeams.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} (Available · {TEAM_TYPE_LABEL[t.teamType] ?? t.teamType} · Cap {t.capacity})
                    </option>
                  ))}
                </select>
                <ChevronDown className="pointer-events-none absolute right-3.5 h-4 w-4 text-slate-400" />
              </div>
              <p className="text-xs text-slate-400">Leave empty to assign a team later from Rescue teams.</p>
            </div>
          </section>

          {/* Api Conflict & Error Notice */}
          <ApiErrorNotice error={create.error ?? (attempted && errors.event ? new Error(errors.event) : null)}>
            {alternatives.length > 0 && (
              <div className="mt-2 space-y-1">
                <p className="text-xs font-semibold text-amber-300">Suggested alternative available teams:</p>
                <ul className="flex flex-wrap gap-2">
                  {alternatives.map((a) => (
                    <li key={a.id}>
                      <button
                        type="button"
                        className="rounded border border-cyan-500/40 bg-cyan-950/60 px-2.5 py-1 text-xs font-medium text-cyan-300 transition-colors hover:bg-cyan-900/60 hover:text-white"
                        onClick={() => {
                          setTeamId(a.id)
                          create.reset()
                        }}
                      >
                        Use {a.name}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </ApiErrorNotice>

          {/* Dispatch Actions */}
          <div className="flex items-center justify-end gap-3 pt-1">
            {!online && (
              <span role="status" className="mr-auto text-sm text-amber-400">
                Dispatching needs a connection. Your form stays here; send it when you are back online.
              </span>
            )}
            <Link
              to={paths.district.teams}
              className="rounded-lg border border-[#232c3f] bg-[#101726] px-5 py-2.5 text-sm font-semibold text-slate-300 transition-colors hover:bg-[#162033] hover:text-white"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={create.isPending || !online}
              className="inline-flex items-center gap-2 rounded-lg bg-cyan-500 px-6 py-2.5 text-sm font-bold text-slate-950 shadow-[0_0_16px_rgba(6,182,212,0.3)] transition-all hover:bg-cyan-400 hover:shadow-[0_0_24px_rgba(76,215,246,0.45)] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {create.isPending ? (
                <span className="flex items-center gap-2">
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-950 border-t-transparent" />
                  <span>Processing...</span>
                </span>
              ) : (
                <>
                  <Send className="h-4 w-4" />
                  <span>{teamId ? 'Create and dispatch' : 'Create assignment'}</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </form>
  )
}
