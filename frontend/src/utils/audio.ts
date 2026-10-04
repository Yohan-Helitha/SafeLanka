let ctx: AudioContext | null = null

/** Two-tone siren, played once. Browsers may block autoplay; callers ignore the failure. */
export async function playAlertTone(): Promise<void> {
  try {
    ctx ??= new AudioContext()
    if (ctx.state === 'suspended') await ctx.resume()
    const start = ctx.currentTime
    ;[880, 660, 880, 660].forEach((freq, i) => {
      const osc = ctx!.createOscillator()
      const gain = ctx!.createGain()
      osc.type = 'square'
      osc.frequency.value = freq
      gain.gain.setValueAtTime(0.0001, start + i * 0.35)
      gain.gain.exponentialRampToValueAtTime(0.18, start + i * 0.35 + 0.03)
      gain.gain.exponentialRampToValueAtTime(0.0001, start + i * 0.35 + 0.32)
      osc.connect(gain).connect(ctx!.destination)
      osc.start(start + i * 0.35)
      osc.stop(start + i * 0.35 + 0.34)
    })
  } catch {
    /* autoplay blocked or audio unavailable */
  }
}
