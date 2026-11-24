import { defineConfig, minimal2023Preset } from '@vite-pwa/assets-generator/config';

// Fundo opaco #F8F9FF (mesmo background_color do manifesto) para os ícones maskable e apple-touch;
// o preset minimal-2023 já aplica a safe-zone (padding) neles. Os ícones "any" ficam transparentes.
export default defineConfig({
  preset: {
    ...minimal2023Preset,
    maskable: { ...minimal2023Preset.maskable, resizeOptions: { background: '#F8F9FF', fit: 'contain' } },
    apple: { ...minimal2023Preset.apple, resizeOptions: { background: '#F8F9FF', fit: 'contain' } },
  },
  images: ['branding/logo-lions.png'],
});
