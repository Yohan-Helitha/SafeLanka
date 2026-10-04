import type {
  Audience,
  Channel,
  ChannelSetting,
  CitizenAlert,
  Delivery,
  DeliveryFilter,
  DeliverySummary,
  EscalateInput,
  HazardDetail,
  HazardInput,
  HazardListItem,
  HazardStatus,
  Page,
  Sensor,
  Warning,
  WarningInput,
  WarningListItem,
  WarningStatus,
  WarningUpdate,
} from '@/types'

export interface SensorTick {
  sensor: Sensor
  /** Set when this reading crossed the alert level and opened a hazard. The simulator never issues a warning. */
  newHazardId: string | null
}

export interface WarningsApi {
  hazards(filter?: { includeResolved?: boolean }): Promise<HazardListItem[]>
  hazard(id: string): Promise<HazardDetail>
  createHazard(input: HazardInput): Promise<HazardDetail>
  setHazardStatus(id: string, status: HazardStatus): Promise<HazardDetail>
  audience(districtIds: string[], basinIds: string[]): Promise<Audience>
  publish(input: WarningInput): Promise<Warning>
  update(id: string, input: WarningUpdate): Promise<Warning>
  escalate(id: string, input: EscalateInput): Promise<Warning>
  cancel(id: string, reason: string): Promise<Warning>
  list(status?: WarningStatus | 'ALL'): Promise<WarningListItem[]>
  get(id: string): Promise<Warning>
  deliveries(
    id: string,
    filter: DeliveryFilter,
  ): Promise<{ summary: DeliverySummary; items: Page<Delivery> }>
  myAlerts(): Promise<CitizenAlert[]>
  simulation: {
    sensors(): Promise<Sensor[]>
    tick(sensorId: string): Promise<SensorTick>
    setReading(sensorId: string, value: number): Promise<SensorTick>
    channels(): Promise<ChannelSetting[]>
    setChannel(
      channel: Channel,
      patch: Partial<Pick<ChannelSetting, 'enabled' | 'simulateFailure'>>,
    ): Promise<ChannelSetting>
  }
}
