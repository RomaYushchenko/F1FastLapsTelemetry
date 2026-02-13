import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client expects Node's `global`; in browser use globalThis
    global: 'globalThis',
  },
})
