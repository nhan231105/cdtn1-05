import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), 'VITE_');
  return {
    plugins: [react()],
    server: { port: 5173, strictPort: true, proxy: { '/api': process.env.VITE_API_TARGET || environment.VITE_API_TARGET || 'http://127.0.0.1:8080' } },
    test: { environment: 'jsdom', setupFiles: ['./src/test-setup.js'], include: ['src/**/*.test.{js,jsx}'] },
  };
});
