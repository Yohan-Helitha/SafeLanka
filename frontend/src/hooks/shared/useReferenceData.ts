import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { api } from '@/services'
import { db } from '@/services/mocks/db'

async function loadAll() {
  const [districts, riverBasins, hazardTypes, organisations, reliefItems, events] = await Promise.all([
    api.reference.districts().catch(() => db.districts),
    api.reference.riverBasins().catch(() => db.riverBasins),
    api.reference.hazardTypes().catch(() => db.hazardTypes),
    api.reference.organisations().catch(() => db.organisations),
    api.reference.reliefItems().catch(() => db.reliefItems),
    api.reference.events().catch(() => db.events),
  ])
  return {
    districts: districts && districts.length > 0 ? districts : db.districts,
    riverBasins: riverBasins && riverBasins.length > 0 ? riverBasins : db.riverBasins,
    hazardTypes: hazardTypes && hazardTypes.length > 0 ? hazardTypes : db.hazardTypes,
    organisations: organisations && organisations.length > 0 ? organisations : db.organisations,
    reliefItems: reliefItems && reliefItems.length > 0 ? reliefItems : db.reliefItems,
    events: events && events.length > 0 ? events : db.events,
  }
}

/** Reference data with lookup helpers. Cached for 10 minutes. */
export function useReferenceData() {
  const query = useQuery({ queryKey: ['reference', 'all'], queryFn: loadAll, staleTime: 10 * 60_000 })
  const data = query.data

  return useMemo(() => {
    const districtList = data?.districts ?? db.districts
    const basinList = data?.riverBasins ?? db.riverBasins
    const hazardList = data?.hazardTypes ?? db.hazardTypes
    const eventList = data?.events ?? db.events
    const orgList = data?.organisations ?? db.organisations
    const reliefList = data?.reliefItems ?? db.reliefItems

    const districtName = (id: string | null | undefined) => {
      if (!id) return '—'
      const norm = String(id).toLowerCase()
      return districtList.find((d) => String(d.id).toLowerCase() === norm)?.name ?? '—'
    }
    const basinName = (id: string | null | undefined) => {
      if (!id) return '—'
      const norm = String(id).toLowerCase()
      return basinList.find((b) => String(b.id).toLowerCase() === norm)?.name ?? '—'
    }
    const hazardTypeName = (id: string | null | undefined) => {
      if (!id) return '—'
      const norm = String(id).toLowerCase()
      return hazardList.find((h) => String(h.id).toLowerCase() === norm)?.name ?? '—'
    }
    const eventName = (id: string | null | undefined) => {
      if (!id) return '—'
      const norm = String(id).toLowerCase()
      return eventList.find((e) => String(e.id).toLowerCase() === norm)?.name ?? '—'
    }
    const organisationName = (id: string | null | undefined) => {
      if (!id) return '—'
      const norm = String(id).toLowerCase()
      return orgList.find((o) => String(o.id).toLowerCase() === norm)?.name ?? '—'
    }
    /** "Colombo · Kelani Ganga basin" for a hazard or warning area. */
    const areaName = (districtId: string | null | undefined, basinId: string | null | undefined) =>
      [districtId ? districtName(districtId) : null, basinId ? `${basinName(basinId)} basin` : null]
        .filter(Boolean)
        .join(' · ') || '—'
    return {
      ...query,
      districts: districtList,
      riverBasins: basinList,
      hazardTypes: hazardList,
      activeHazardTypes: hazardList.filter((h) => h.active),
      organisations: orgList,
      reliefItems: reliefList,
      events: eventList,
      activeEvents: eventList.filter((e) => e.status === 'ACTIVE'),
      districtName,
      basinName,
      hazardTypeName,
      eventName,
      organisationName,
      areaName,
    }
  }, [query, data])
}
