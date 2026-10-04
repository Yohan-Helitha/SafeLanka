import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { api } from '@/services'

async function loadAll() {
  const [districts, riverBasins, hazardTypes, organisations, reliefItems, events] = await Promise.all([
    api.reference.districts(),
    api.reference.riverBasins(),
    api.reference.hazardTypes(),
    api.reference.organisations(),
    api.reference.reliefItems(),
    api.reference.events(),
  ])
  return { districts, riverBasins, hazardTypes, organisations, reliefItems, events }
}

/** Reference data with lookup helpers. Cached for 10 minutes. */
export function useReferenceData() {
  const query = useQuery({ queryKey: ['reference', 'all'], queryFn: loadAll, staleTime: 10 * 60_000 })
  const data = query.data

  return useMemo(() => {
    const districtName = (id: string | null | undefined) => data?.districts.find((d) => d.id === id)?.name ?? '—'
    const basinName = (id: string | null | undefined) => data?.riverBasins.find((b) => b.id === id)?.name ?? '—'
    const hazardTypeName = (id: string | null | undefined) => data?.hazardTypes.find((h) => h.id === id)?.name ?? '—'
    const eventName = (id: string | null | undefined) => data?.events.find((e) => e.id === id)?.name ?? '—'
    const organisationName = (id: string | null | undefined) =>
      data?.organisations.find((o) => o.id === id)?.name ?? '—'
    /** "Colombo · Kelani Ganga basin" for a hazard or warning area. */
    const areaName = (districtId: string | null | undefined, basinId: string | null | undefined) =>
      [districtId ? districtName(districtId) : null, basinId ? `${basinName(basinId)} basin` : null]
        .filter(Boolean)
        .join(' · ') || '—'
    return {
      ...query,
      districts: data?.districts ?? [],
      riverBasins: data?.riverBasins ?? [],
      hazardTypes: data?.hazardTypes ?? [],
      activeHazardTypes: data?.hazardTypes.filter((h) => h.active) ?? [],
      organisations: data?.organisations ?? [],
      reliefItems: data?.reliefItems ?? [],
      events: data?.events ?? [],
      activeEvents: data?.events.filter((e) => e.status === 'ACTIVE') ?? [],
      districtName,
      basinName,
      hazardTypeName,
      eventName,
      organisationName,
      areaName,
    }
  }, [query, data])
}
