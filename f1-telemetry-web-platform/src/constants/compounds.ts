/**
 * F1 25 tyre compound code → display label. Used in Strategy View (pit stops, stints).
 * Codes: 16=C5 (soft), 18=C3 (medium), etc.; 7=Inter, 8=Wet.
 */

const COMPOUND_LABELS: Record<number, string> = {
  7: 'Inter',
  8: 'Wet',
  16: 'C5',
  17: 'C4',
  18: 'C3',
  19: 'C2',
  20: 'C1',
}

export function getCompoundLabel(code: number | null | undefined): string {
  if (code == null) return '—'
  return COMPOUND_LABELS[code] ?? `C${code}`
}
