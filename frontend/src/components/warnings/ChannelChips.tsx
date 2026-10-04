import clsx from 'clsx'
import { CHANNEL_LABEL } from '@/constants/labels'
import { SEVERITY } from '@/theme/tokens'
import type { Channel, ChannelSetting, WarningLevel } from '@/types'

interface Props {
  settings: ChannelSetting[]
  level: WarningLevel
}

const ORDER: Channel[] = ['PUSH', 'SMS', 'AUDIBLE']

/** Which channels will carry the warning, and which are off or failing in the simulation. */
export function ChannelChips({ settings, level }: Props) {
  return (
    <ul className="flex flex-wrap gap-2">
      {ORDER.map((channel) => {
        const s = settings.find((x) => x.channel === channel)
        const levelBlocks = channel === 'AUDIBLE' && !SEVERITY[level].audible
        let note = ''
        let tone = 'border-ok/50 text-ok'
        if (!s?.enabled) {
          note = 'off'
          tone = 'border-line text-faint'
        } else if (levelBlocks) {
          note = 'only for Warning and Evacuate'
          tone = 'border-line text-faint'
        } else if (s.simulateFailure) {
          note = 'failing — others continue'
          tone = 'border-caution/60 text-caution'
        }
        return (
          <li key={channel} className={clsx('rounded-full border px-3 py-1 text-sm', tone)}>
            {CHANNEL_LABEL[channel]}
            {note && <span className="ml-1.5 opacity-80">· {note}</span>}
          </li>
        )
      })}
    </ul>
  )
}
