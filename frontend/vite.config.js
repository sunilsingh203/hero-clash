import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The Spring Boot backend runs on :8080. Proxy REST + the SockJS endpoint so the
// browser only ever talks to the Vite origin during development.
export default defineConfig({
  plugins: [react()],
  // sockjs-client expects a Node-style `global`.
  define: { global: 'globalThis' },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'http://localhost:8080', ws: true },
    },
  },
});
