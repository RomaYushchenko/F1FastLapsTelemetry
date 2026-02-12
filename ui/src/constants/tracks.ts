const TRACK_NAMES: Record<number, string> = {
  1: 'Melbourne',
  2: 'Paul Ricard',
  3: 'Shanghai',
  4: 'Sakhir (Bahrain)',
  5: 'Catalunya',
  6: 'Monaco',
  7: 'Montreal',
  8: 'Silverstone',
  9: 'Hockenheim',
  10: 'Hungaroring',
  11: 'Spa-Francorchamps',
  12: 'Monza',
  13: 'Singapore',
  14: 'Suzuka',
  15: 'Abu Dhabi',
  16: 'Texas (COTA)',
  17: 'Brazil (Interlagos)',
  18: 'Austria (Red Bull Ring)',
  19: 'Sochi',
  20: 'Mexico City',
  21: 'Baku',
  22: 'Sakhir Short',
  23: 'Silverstone Short',
  24: 'Texas Short',
  25: 'Suzuka Short',
  26: 'Hanoi',
  27: 'Zandvoort',
  28: 'Imola',
  29: 'Portimão',
  30: 'Jeddah',
}

export function getTrackName(trackId: number): string {
  const name = TRACK_NAMES[trackId]
  if (name) return name
  return `Track #${trackId}`
}

