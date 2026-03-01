/**
 * In-memory notification list for the Bell dropdown.
 * Same events as toasts (errors, success, etc.) are pushed here so the Bell shows recent notifications.
 */

const MAX_ITEMS = 20

export interface NotificationItem {
  id: string
  type: 'error' | 'success' | 'warning' | 'info'
  message: string
  timestamp: number
  read?: boolean
}

let items: NotificationItem[] = []
const listeners = new Set<(list: NotificationItem[]) => void>()

function emit() {
  listeners.forEach((fn) => fn([...items]))
}

export function getNotifications(): NotificationItem[] {
  return [...items]
}

export function addNotification(
  item: Omit<NotificationItem, 'id' | 'timestamp'>
) {
  items = [
    { ...item, id: crypto.randomUUID(), timestamp: Date.now(), read: false },
    ...items,
  ].slice(0, MAX_ITEMS)
  emit()
}

export function markAllAsRead() {
  items = items.map((i) => ({ ...i, read: true }))
  emit()
}

export function subscribe(fn: (list: NotificationItem[]) => void): () => void {
  listeners.add(fn)
  fn([...items])
  return () => listeners.delete(fn)
}
