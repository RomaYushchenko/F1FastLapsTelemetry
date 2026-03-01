/**
 * Session type filter options for Session History. Values match backend F1SessionType enum names.
 */

export const SESSION_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: 'RACE', label: 'Race' },
  { value: 'QUALIFYING_1', label: 'Qualifying 1' },
  { value: 'QUALIFYING_2', label: 'Qualifying 2' },
  { value: 'QUALIFYING_3', label: 'Qualifying 3' },
  { value: 'PRACTICE_1', label: 'Practice 1' },
  { value: 'PRACTICE_2', label: 'Practice 2' },
  { value: 'PRACTICE_3', label: 'Practice 3' },
  { value: 'SPRINT_SHOOTOUT_1', label: 'Sprint Shootout 1' },
  { value: 'SPRINT_SHOOTOUT_2', label: 'Sprint Shootout 2' },
  { value: 'SHORT_PRACTICE', label: 'Short Practice' },
  { value: 'SHORT_QUALIFYING', label: 'Short Qualifying' },
  { value: 'TIME_TRIAL', label: 'Time Trial' },
]
