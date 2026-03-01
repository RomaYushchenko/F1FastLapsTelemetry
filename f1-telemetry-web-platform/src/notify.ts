/**
 * Global notifications: show toast (Sonner) and push to Bell list (notificationStore).
 * Use this instead of raw toast so the header Bell dropdown stays in sync.
 */

import { toast } from 'sonner'
import { addNotification } from './notificationStore'

export const notify = {
  error: (message: string) => {
    toast.error(message)
    addNotification({ type: 'error', message })
  },
  success: (message: string) => {
    toast.success(message)
    addNotification({ type: 'success', message })
  },
  warning: (message: string) => {
    toast.warning(message)
    addNotification({ type: 'warning', message })
  },
  info: (message: string) => {
    toast.info(message)
    addNotification({ type: 'info', message })
  },
}
