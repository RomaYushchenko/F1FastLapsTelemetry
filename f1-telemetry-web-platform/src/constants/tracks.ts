/**
 * Track names by trackId (F1 25 game ids).
 * Must stay in sync with backend enum F1Track (telemetry-api-contracts).
 */

const TRACK_NAMES: Record<number, string> = {
  0: 'Melbourne',
  2: 'Shanghai',
  3: 'Sakhir (Bahrain)',
  4: 'Catalunya',
  5: 'Monaco',
  6: 'Montreal',
  7: 'Silverstone',
  9: 'Hungaroring',
  10: 'Spa',
  11: 'Monza',
  12: 'Singapore',
  13: 'Suzuka',
  14: 'Abu Dhabi',
  15: 'Texas',
  16: 'Brazil',
  17: 'Austria',
  19: 'Mexico',
  20: 'Baku (Azerbaijan)',
  26: 'Zandvoort',
  27: 'Imola',
  29: 'Jeddah',
  30: 'Miami',
  31: 'Las Vegas',
  32: 'Losail',
  39: 'Silverstone (Reverse)',
  40: 'Austria (Reverse)',
  41: 'Zandvoort (Reverse)',
}

export function getTrackName(trackId: number | null | undefined): string {
  if (trackId == null) return '—'
  const name = TRACK_NAMES[trackId]
  if (name) return name
  return `Track #${trackId}`
}

/** Track options for Session History filter dropdown (trackId → label). */
export const TRACK_OPTIONS: { value: number; label: string }[] = Object.entries(
  TRACK_NAMES
).map(([id, label]) => ({ value: Number(id), label }))
