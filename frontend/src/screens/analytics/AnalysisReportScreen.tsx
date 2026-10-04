import { Download } from 'lucide-react'
import { useParams } from 'react-router-dom'
import { AlertTimelineSection } from '@/components/analytics/AlertTimelineSection'
import { CitizensReachedSection } from '@/components/analytics/CitizensReachedSection'
import { ResourceDistributionSection } from '@/components/analytics/ResourceDistributionSection'
import { SectionNotice } from '@/components/analytics/SectionNotice'
import { ShelterOccupancySection } from '@/components/analytics/ShelterOccupancySection'
import { ApiErrorNotice } from '@/components/domain'
import { Button, ErrorState, Loading, PageHeader } from '@/components/ui'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useDisasterReport, useDownloadReport } from '@/hooks/analytics/useAnalytics'
import type { SectionKey } from '@/types'
import { fileStamp, formatDate, formatDateTime, slug } from '@/utils/format'

interface Props {
  backTo: string
}

export function AnalysisReportScreen({ backTo }: Props) {
  const { id = '' } = useParams()
  const { districtName } = useReferenceData()
  const report = useDisasterReport(id)
  const download = useDownloadReport()
  useDocumentTitle(report.data?.eventName ?? 'Analysis report')

  if (report.isLoading) return <Loading label="Loading report" />
  if (report.isError || !report.data) return <ErrorState error={report.error} onRetry={() => void report.refetch()} />
  const r = report.data
  const reason = (key: SectionKey) => r.unavailableSections.find((s) => s.key === key)?.reason ?? 'No data was recorded.'
  const filename = `disaster-report-${slug(r.eventName)}-${fileStamp(r.generatedAt)}`
  const range = r.filters.from || r.filters.to ? ` · ${r.filters.from ? formatDate(r.filters.from) : 'start'} – ${r.filters.to ? formatDate(r.filters.to) : 'now'}` : ''

  return (
    <div>
      <PageHeader
        backTo={backTo}
        backLabel="Analysis"
        title={r.eventName}
        subtitle={`Generated ${formatDateTime(r.generatedAt)} by ${r.generatedBy} · ${r.filters.districtIds.map(districtName).join(', ')}${range}`}
        actions={
          <>
            <Button variant="secondary" loading={download.isPending && download.variables?.format === 'CSV'} onClick={() => download.mutate({ id: r.id, format: 'CSV', filename })}>
              CSV
            </Button>
            <Button icon={<Download className="size-4" aria-hidden />} loading={download.isPending && download.variables?.format === 'PDF'} onClick={() => download.mutate({ id: r.id, format: 'PDF', filename })}>
              PDF
            </Button>
          </>
        }
      />
      <div className="mb-4">
        <ApiErrorNotice error={download.error} />
      </div>
      <div className="space-y-4">
        {r.alertTimeline ? <AlertTimelineSection data={r.alertTimeline} districtName={districtName} /> : <SectionNotice name="Alert timeline" reason={reason('alertTimeline')} />}
        {r.citizensReached ? <CitizensReachedSection data={r.citizensReached} districtName={districtName} /> : <SectionNotice name="Citizens reached" reason={reason('citizensReached')} />}
        {r.shelterOccupancy ? <ShelterOccupancySection data={r.shelterOccupancy} /> : <SectionNotice name="Shelter occupancy over time" reason={reason('shelterOccupancy')} />}
        {r.resourceDistribution ? <ResourceDistributionSection data={r.resourceDistribution} /> : <SectionNotice name="Resource distribution" reason={reason('resourceDistribution')} />}
      </div>
    </div>
  )
}
